package art.aelaort;

import art.aelaort.models.DirServer;
import art.aelaort.models.Server;
import art.aelaort.models.TabbyServer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataService {
	public List<Server> join(List<DirServer> dirServers, List<TabbyServer> tabbyServers) {
		Map<String, DirServer> mapServers = toMapServers(dirServers);
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

		Map<String, TabbyServer> mapTabbyServers = toMapTabby(tabbyServers);
		for (DirServer dirServer : dirServers) {
			if (!mapTabbyServers.containsKey(dirServer.name())) {
				result.add(new Server(dirServer.name(), "-", "-", -1, dirServer.monitoring(), dirServer.services()));
			}
		}

		return result;
	}

	public Map<String, DirServer> toMapServers(List<DirServer> dirServers) {
		Map<String, DirServer> result = new HashMap<>();
		dirServers.forEach(dirServer -> result.put(dirServer.name(), dirServer));
		return result;
	}

	public Map<String, TabbyServer> toMapTabby(List<TabbyServer> tabbyServers) {
		Map<String, TabbyServer> result = new HashMap<>();
		tabbyServers.forEach(tabbyServer -> result.put(tabbyServer.name(), tabbyServer));
		return result;
	}
}
