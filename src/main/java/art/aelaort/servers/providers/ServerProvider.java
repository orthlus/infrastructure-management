package art.aelaort.servers.providers;

import art.aelaort.mappers.ServerMapper;
import art.aelaort.models.servers.DirServer;
import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.TabbyServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ServerProvider {
	private final ServerMapper serverMapper;

	public List<Server> joinDirAndTabbyServers(List<DirServer> dirServers, List<TabbyServer> tabbyServers) {
		Map<String, DirServer> mapServers = serverMapper.toMapServers(dirServers);
		List<Server> result = new ArrayList<>(tabbyServers.size());

		for (TabbyServer tabbyServer : tabbyServers) {
			Server.ServerBuilder serverBuilder = Server.builder()
					.name(tabbyServer.name())
					.ip(tabbyServer.host())
					.port(tabbyServer.port())
					.sshKey(tabbyServer.keyPath().replace("\\", "/"));

			DirServer dirServer = mapServers.get(tabbyServer.name());
			if (dirServer == null) {
				serverBuilder
						.monitoring(false)
						.services(List.of());
			} else {
				serverBuilder
						.monitoring(dirServer.monitoring())
						.services(dirServer.services());
			}
			result.add(serverBuilder.build());
		}

		Map<String, TabbyServer> mapTabbyServers = serverMapper.toMapTabby(tabbyServers);
		for (DirServer dirServer : dirServers) {
			if (!mapTabbyServers.containsKey(dirServer.name())) {
				Server server = Server.builder()
						.name(dirServer.name())
						.monitoring(dirServer.monitoring())
						.services(dirServer.services())
						.build();
				result.add(server);
			}
		}

		return Server.addNumbers(result);
	}
}
