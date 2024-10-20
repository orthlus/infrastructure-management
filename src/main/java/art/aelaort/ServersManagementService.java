package art.aelaort;

import art.aelaort.data.parsers.CustomProjectYamlParser;
import art.aelaort.data.parsers.DockerComposeParser;
import art.aelaort.mappers.ServerMapper;
import art.aelaort.models.servers.DirServer;
import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.TabbyServer;
import art.aelaort.s3.ServersManagementS3;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ServersManagementService {
	private final ServersManagementS3 serversManagementS3;
	private final TabbyFiles tabbyFiles;
	private final DockerComposeParser dockerComposeParser;
	private final CustomProjectYamlParser customProjectYamlParser;
	private final JsonMapper jsonMapper;
	private final ServerMapper serverMapper;
	@Value("${servers.management.dir}")
	private Path serversDir;
	@Value("${servers.management.files.not_scan}")
	private String notScanFile;
	@Value("${servers.management.custom_projects_file}")
	private String projectsYmlFileName;
	@Value("${servers.management.json_path}")
	private Path jsonDataPath;

	public void saveIps(List<Server> servers) {
		String text = servers.stream()
				.filter(Server::isMonitoring)
				.map(server -> server.getName() + ":" + server.getIp() + ":" + server.getPort())
				.collect(Collectors.joining("\n"))
				+ "\n";
		serversManagementS3.uploadIps(text);
		log("ips uploaded");
	}

	public void saveData(List<Server> servers) {
		String json = toJson(servers);
		saveJsonToLocal(json);
		log("saved data to local");
		serversManagementS3.uploadData(json);
		log("saved data to s3");
	}

	@SneakyThrows
	public List<Server> readLocalJsonData() {
		return serversParse(jsonDataPath);
	}

	public List<Server> scanOnlyLocalData() {
		List<DirServer> dirServers = scanLocalFilesInServersDir();
		List<TabbyServer> tabbyServers = tabbyFiles.readLocal();
		return joinDirAndTabbyServers(dirServers, tabbyServers);
	}

	public List<Server> scanAndJoinData() {
		List<DirServer> dirServers = scanLocalFilesInServersDir();
		List<TabbyServer> tabbyServers = tabbyFiles.readRemote();
		return joinDirAndTabbyServers(dirServers, tabbyServers);
	}

	public List<DirServer> scanLocalFilesInServersDir() {
		List<Path> serversDirs = scanLocalDirs();
		List<DirServer> result = new ArrayList<>(serversDirs.size() * 2);

		for (Path serverDir : serversDirs) {
			for (Path ymlFile : findYmlFiles(serverDir)) {
				String file = ymlFile.getFileName().toString();
				if (file.contains("docker")) {
					result.add(dockerComposeParser.parseDockerYmlFile(ymlFile));
				} else if (file.equals(projectsYmlFileName)) {
					result.add(customProjectYamlParser.parseCustomYmlFile(ymlFile));
				}
			}
		}

		return result;
	}

	public List<Server> joinDirAndTabbyServers(List<DirServer> dirServers, List<TabbyServer> tabbyServers) {
		Map<String, DirServer> mapServers = serverMapper.toMapServers(dirServers);
		List<Server> result = new ArrayList<>(tabbyServers.size());

		for (TabbyServer tabbyServer : tabbyServers) {
			DirServer dirServer = mapServers.get(tabbyServer.name());
			String sshKey = tabbyServer.keyPath().replace("\\", "/");
			if (dirServer == null) {
				result.add(new Server(tabbyServer.name(), tabbyServer.host(), sshKey, tabbyServer.port(), false, List.of()));
			} else {
				result.add(new Server(tabbyServer.name(), tabbyServer.host(), sshKey, tabbyServer.port(), dirServer.monitoring(), dirServer.services()));
			}
		}

		Map<String, TabbyServer> mapTabbyServers = serverMapper.toMapTabby(tabbyServers);
		for (DirServer dirServer : dirServers) {
			if (!mapTabbyServers.containsKey(dirServer.name())) {
				result.add(new Server(dirServer.name(), "-", "-", -1, dirServer.monitoring(), dirServer.services()));
			}
		}

		return result;
	}

	@SneakyThrows
	private String toJson(List<Server> server) {
		return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(server);
	}

	@SneakyThrows
	private List<Server> serversParse(Path jsonPath) {
		return List.of(jsonMapper.readValue(jsonPath.toFile(), Server[].class));
	}

	@SneakyThrows
	private void saveJsonToLocal(String jsonStr) {
		Files.writeString(jsonDataPath, jsonStr);
	}

	@SneakyThrows
	private List<Path> findYmlFiles(Path dir) {
		return Files.walk(dir, 1)
				.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".yml"))
				.toList();
	}

	@SneakyThrows
	private List<Path> scanLocalDirs() {
		return Files.walk(serversDir, 1)
				.filter(path -> !path.equals(serversDir))
				.filter(path -> path.toFile().isDirectory())
				.filter(path -> !path.resolve(notScanFile).toFile().exists())
				.toList();
	}
}
