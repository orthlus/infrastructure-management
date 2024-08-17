package art.aelaort;

import art.aelaort.models.servers.DirServer;
import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.ServiceDto;
import art.aelaort.models.servers.TabbyServer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

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
	private final Yaml yaml;
	private final ServersManagementS3 serversManagementS3;
	private final SerializeService serializeService;
	private final ServerJoiner serverJoiner;
	private final TabbyService tabbyService;
	@Value("${servers.management.dir}")
	private String serversDir;
	@Value("${servers.management.files.monitoring}")
	private String monitoringFile;
	@Value("${servers.management.files.not_scan}")
	private String notScanFile;
	@Value("${servers.management.custom_projects_file}")
	private String projectsYmlFileName;
	@Value("${servers.management.json_path}")
	private String jsonDataPath;
	@Value("${servers.management.docker.image.pattern}")
	private String dockerImagePattern;

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
		String json = Files.readString(Path.of(jsonDataPath));
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

	@SuppressWarnings("unchecked")
	private DirServer parseCustomYmlFile(Path ymlFile) {
		Path serverDir = ymlFile.getParent();
		boolean monitoring = serverDir.resolve(monitoringFile).toFile().exists();
		String content = readFile(ymlFile);

		Map<String, Object> load = yaml.load(content);
		List<String> projects = (List<String>) load.get("projects");
		List<ServiceDto> services = new ArrayList<>();
		for (String project : projects) {
			services.add(new ServiceDto(project, ymlFile.getFileName().toString()));
		}
		return new DirServer(serverDir.getFileName().toString(), monitoring, services);
	}

	@SuppressWarnings("unchecked")
	private DirServer parseDockerYmlFile(Path ymlFile) {
		Path serverDir = ymlFile.getParent();
		boolean monitoring = serverDir.resolve(monitoringFile).toFile().exists();
		String content = readFile(ymlFile);

		Map<String, Object> load = yaml.load(content);
		Map<String, Object> services = (Map<String, Object>) load.get("services");

		List<ServiceDto> resultServices = services.entrySet()
				.stream()
				.map(service -> getServiceDto(ymlFile, service))
				.toList();

		return new DirServer(serverDir.getFileName().toString(), monitoring, resultServices);
	}

	@SuppressWarnings("unchecked")
	private ServiceDto getServiceDto(Path ymlFile, Map.Entry<String, Object> service) {
		String serviceName = service.getKey();
		Map<String, Object> params = (Map<String, Object>) service.getValue();

		ServiceDto serviceDto;
		if (params.containsKey("container_name")) {
			String containerName = (String) params.get("container_name");
			serviceDto = new ServiceDto(containerName, ymlFile.getFileName().toString(), serviceName);
		} else {
			serviceDto = new ServiceDto(serviceName, ymlFile.getFileName().toString());
		}

		if (params.containsKey("image")) {
			String[] split = dockerImagePattern.split("%%");
			String image = ((String) params.get("image"))
					.replace(split[0], "")
					.replace(split[1], "");
			serviceDto.setDockerImageName(image);
		}

		return serviceDto;
	}

	@SneakyThrows
	public void saveJsonToLocal(String jsonStr) {
		Files.writeString(Path.of(jsonDataPath), jsonStr);
	}

	@SneakyThrows
	private String readFile(Path ymlFile) {
		return Files.readString(ymlFile);
	}

	@SneakyThrows
	private List<Path> findYmlFiles(Path dir) {
		return Files.walk(dir, 1)
				.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".yml"))
				.toList();
	}

	@SneakyThrows
	public List<Path> scanLocalDirs() {
		Path dir = Path.of(serversDir);
		return Files.walk(dir, 1)
				.filter(path -> !path.equals(dir))
				.filter(path -> path.toFile().isDirectory())
				.filter(path -> !path.resolve(notScanFile).toFile().exists())
				.toList();
	}
}
