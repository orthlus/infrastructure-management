package art.aelaort;

import art.aelaort.models.servers.DirServer;
import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.ServiceDto;
import art.aelaort.models.servers.TabbyServer;
import art.aelaort.models.servers.yaml.CustomFile;
import art.aelaort.models.servers.yaml.DockerComposeFile;
import art.aelaort.s3.ServersManagementS3;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServersManagementService {
	private final ServersManagementS3 serversManagementS3;
	private final SerializeService serializeService;
	private final ServerJoiner serverJoiner;
	private final TabbyService tabbyService;
	private final ObjectMapper yamlMapper;
	@Value("${servers.management.dir}")
	private Path serversDir;
	@Value("${servers.management.files.monitoring}")
	private String monitoringFile;
	@Value("${servers.management.files.not_scan}")
	private String notScanFile;
	@Value("${servers.management.custom_projects_file}")
	private String projectsYmlFileName;
	@Value("${servers.management.json_path}")
	private Path jsonDataPath;
	@Value("${servers.management.docker.image.pattern}")
	private String dockerImagePattern;
	private String[] dockerImagePatternSplit;

	@PostConstruct
	private void init() {
		dockerImagePatternSplit = dockerImagePattern.split("%%");
	}

	public void saveIps(List<Server> servers) {
		String text = servers.stream()
				.filter(Server::isMonitoring)
				.map(server -> server.getName() + ":" + server.getIp() + ":" + server.getPort())
				.collect(Collectors.joining("\n"))
				+ "\n";
		serversManagementS3.uploadIps(text);
		System.out.println("ips uploaded");
	}

	public void saveData(List<Server> servers) {
		String json = serializeService.toJson(servers);
		saveJsonToLocal(json);
		System.out.println("saved data to local");
		serversManagementS3.uploadData(json);
		System.out.println("saved data to s3");
	}

	@SneakyThrows
	public List<Server> readLocalJsonData() {
		String json = Files.readString(jsonDataPath);
		return serializeService.serversParse(json);
	}

	public List<Server> syncData(boolean logging) {
		List<DirServer> dirServers = scanLocalFilesInServersDir();
		tabbyService.downloadFileToLocal(logging);
		List<TabbyServer> tabbyServers = tabbyService.getServersFromLocalFile();
		return serverJoiner.join(dirServers, tabbyServers);
	}

	public List<Server> scanAndJoinData(boolean logging) {
		List<DirServer> dirServers = scanLocalFilesInServersDir();
		tabbyService.downloadFileToLocal(logging);
		List<TabbyServer> tabbyServers = tabbyService.getServersFromLocalFile();
		return serverJoiner.join(dirServers, tabbyServers);
	}

	public List<DirServer> scanLocalFilesInServersDir() {
		List<Path> serversDirs = scanLocalDirs();
		List<DirServer> result = new ArrayList<>(serversDirs.size() * 2);

		for (Path serverDir : serversDirs) {
			for (Path ymlFile : findYmlFiles(serverDir)) {
				String file = ymlFile.getFileName().toString();
				if (file.contains("docker")) {
					result.add(parseDockerYmlFile(ymlFile));
				} else if (file.equals(projectsYmlFileName)) {
					result.add(parseCustomYmlFile(ymlFile));
				}
			}
		}

		return result;
	}

	private DirServer parseCustomYmlFile(Path ymlFile) {
		try {
			Path serverDir = ymlFile.getParent();
			boolean monitoring = serverDir.resolve(monitoringFile).toFile().exists();
			List<String> projects = yamlMapper.readValue(ymlFile.toFile(), CustomFile.class).getProjects();
			List<ServiceDto> services = new ArrayList<>();
			for (String project : projects) {
				services.add(ServiceDto.builder()
						.service(project)
						.ymlName(ymlFile.getFileName().toString())
						.build());
			}
			return new DirServer(serverDir.getFileName().toString(), monitoring, services);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private DirServer parseDockerYmlFile(Path ymlFile) {
		try {
			Path serverDir = ymlFile.getParent();
			boolean monitoring = serverDir.resolve(monitoringFile).toFile().exists();

			DockerComposeFile file = yamlMapper.readValue(ymlFile.toFile(), DockerComposeFile.class);

			List<ServiceDto> resultServices = new ArrayList<>();
			for (Map.Entry<String, DockerComposeFile.Service> entry : file.getServices().entrySet()) {
				resultServices.add(getServiceDto(ymlFile, entry));
			}

			return new DirServer(serverDir.getFileName().toString(), monitoring, resultServices);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private ServiceDto getServiceDto(Path ymlFile, Map.Entry<String, DockerComposeFile.Service> entry) {
		String serviceName = entry.getKey();
		DockerComposeFile.Service service = entry.getValue();
		ServiceDto.ServiceDtoBuilder serviceDtoBuilder = ServiceDto.builder()
				.ymlName(ymlFile.getFileName().toString());

		String containerName = service.getContainerName();
		if (containerName != null) {
			serviceDtoBuilder
					.service(containerName)
					.dockerName(serviceName);
		} else {
			serviceDtoBuilder.service(serviceName);
		}

		String image = service.getImage();
		if (image != null) {
			serviceDtoBuilder.dockerImageName(dockerImageClean(image));
		}

		return serviceDtoBuilder.build();
	}

	private String dockerImageClean(String dockerImage) {
		return dockerImage
				.replace(dockerImagePatternSplit[0], "")
				.replace(dockerImagePatternSplit[1], "");
	}

	@SneakyThrows
	public void saveJsonToLocal(String jsonStr) {
		Files.writeString(jsonDataPath, jsonStr);
	}

	@SneakyThrows
	private List<Path> findYmlFiles(Path dir) {
		return Files.walk(dir, 1)
				.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".yml"))
				.toList();
	}

	@SneakyThrows
	public List<Path> scanLocalDirs() {
		return Files.walk(serversDir, 1)
				.filter(path -> !path.equals(serversDir))
				.filter(path -> path.toFile().isDirectory())
				.filter(path -> !path.resolve(notScanFile).toFile().exists())
				.toList();
	}
}
