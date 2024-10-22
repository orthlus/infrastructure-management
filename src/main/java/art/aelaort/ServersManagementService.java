package art.aelaort;

import art.aelaort.models.servers.DirServer;
import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.TabbyServer;
import art.aelaort.s3.ServersManagementS3;
import art.aelaort.servers.providers.DirServerProvider;
import art.aelaort.servers.providers.ServerProvider;
import art.aelaort.servers.providers.TabbyServerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ServersManagementService {
	private final ServersManagementS3 serversManagementS3;
	private final TabbyServerProvider tabbyServerProvider;
	private final JsonMapper jsonMapper;
	private final DirServerProvider dirServerProvider;
	private final ServerProvider serverProvider;
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

	public List<Server> readLocalJsonData() {
		return serversParse(jsonDataPath);
	}

	public List<Server> scanOnlyLocalData() {
		List<DirServer> dirServers = dirServerProvider.scanServersDir();
		List<TabbyServer> tabbyServers = tabbyServerProvider.readLocal();
		return serverProvider.joinDirAndTabbyServers(dirServers, tabbyServers);
	}

	public List<Server> scanAndJoinData() {
		List<DirServer> dirServers = dirServerProvider.scanServersDir();
		List<TabbyServer> tabbyServers = tabbyServerProvider.readRemote();
		return serverProvider.joinDirAndTabbyServers(dirServers, tabbyServers);
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
}
