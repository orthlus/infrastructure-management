package art.aelaort.servers.providers;

import art.aelaort.mappers.ServerMapper;
import art.aelaort.models.servers.DirServer;
import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.TabbyServer;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ServerProvider {
	private final ServerMapper serverMapper;
	private final DirServerProvider dirServerProvider;
	private final TabbyServerProvider tabbyServerProvider;
	private final JsonMapper jsonMapper;
	@Value("${servers.management.json_path}")
	private Path jsonDataPath;

	public List<Server> readLocalJsonData() {
		return serversParse(jsonDataPath);
	}

	public List<Server> scanOnlyLocalData() {
		List<DirServer> dirServers = dirServerProvider.scanServersDir();
		List<TabbyServer> tabbyServers = tabbyServerProvider.readLocal();
		return joinDirAndTabbyServers(dirServers, tabbyServers);
	}

	public List<Server> scanAndJoinData() {
		List<DirServer> dirServers = dirServerProvider.scanServersDir();
		List<TabbyServer> tabbyServers = tabbyServerProvider.readRemote();
		return joinDirAndTabbyServers(dirServers, tabbyServers);
	}

	@SneakyThrows
	private List<Server> serversParse(Path jsonPath) {
		return List.of(jsonMapper.readValue(jsonPath.toFile(), Server[].class));
	}

	private List<Server> joinDirAndTabbyServers(List<DirServer> dirServers, List<TabbyServer> tabbyServers) {
		Map<String, DirServer> dirServersByName = serverMapper.toMapServers(dirServers);
		List<Server> result = new ArrayList<>(tabbyServers.size());

		for (TabbyServer tabbyServer : tabbyServers) {
			Server.ServerBuilder serverBuilder = Server.builder()
					.name(tabbyServer.name())
					.ip(tabbyServer.host())
					.port(tabbyServer.port())
					.sshKey(tabbyServer.keyPath().replace("\\", "/"));

			DirServer dirServer = dirServersByName.get(tabbyServer.name());
			if (dirServer == null) {
				serverBuilder
						.monitoring(false)
						.services(List.of());
			} else {
				serverBuilder
						.monitoring(dirServer.monitoring())
						.services(dirServer.services())
						.price(dirServer.price());
			}
			result.add(serverBuilder.build());
		}

		Map<String, TabbyServer> tabbyServersByName = serverMapper.toMapTabby(tabbyServers);
		for (DirServer dirServer : dirServers) {
			if (!tabbyServersByName.containsKey(dirServer.name())) {
				Server server = Server.builder()
						.name(dirServer.name())
						.monitoring(dirServer.monitoring())
						.services(dirServer.services())
						.price(dirServer.price())
						.build();
				result.add(server);
			}
		}

		return Server.addNumbers(result);
	}
}
