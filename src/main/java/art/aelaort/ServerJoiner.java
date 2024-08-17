package art.aelaort;

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
public class ServerJoiner {
	private final ServerMapper serverMapper;

	public List<Server> join(List<DirServer> dirServers, List<TabbyServer> tabbyServers) {
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
}
