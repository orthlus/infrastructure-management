package art.aelaort;

import art.aelaort.models.PhysicalServer;
import art.aelaort.models.Server;
import art.aelaort.models.TabbyServer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataService {
	public List<PhysicalServer> join(List<Server> servers, List<TabbyServer> tabbyServers) {
		Map<String, Server> mapServers = toMapServers(servers);
		List<PhysicalServer> result = new ArrayList<>(tabbyServers.size());

		for (TabbyServer tabbyServer : tabbyServers) {
			Server server = mapServers.get(tabbyServer.name());
			String sshKey = tabbyServer.keyPath().replace("\\", "/");
			if (server == null) {
				result.add(new PhysicalServer(tabbyServer.name(), tabbyServer.host(), sshKey, false, List.of()));
			} else {
				result.add(new PhysicalServer(tabbyServer.name(), tabbyServer.host(), sshKey, server.monitoring(), server.services()));
			}
		}

		Map<String, TabbyServer> mapTabbyServers = toMapTabby(tabbyServers);
		for (Server server : servers) {
			if (!mapTabbyServers.containsKey(server.name())) {
				result.add(new PhysicalServer(server.name(), "null", "null", server.monitoring(), server.services()));
			}
		}

		return result;
	}

	public Map<String, Server> toMapServers(List<Server> servers) {
		Map<String, Server> result = new HashMap<>();
		servers.forEach(server -> result.put(server.name(), server));
		return result;
	}

	public Map<String, TabbyServer> toMapTabby(List<TabbyServer> tabbyServers) {
		Map<String, TabbyServer> result = new HashMap<>();
		tabbyServers.forEach(tabbyServer -> result.put(tabbyServer.name(), tabbyServer));
		return result;
	}
}
