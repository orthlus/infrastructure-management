package art.aelaort;

import art.aelaort.models.PhysicalServer;
import art.aelaort.models.Server;
import art.aelaort.models.TabbyHost;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataService {
	public List<PhysicalServer> join(List<Server> servers, List<TabbyHost> hosts) {
		Map<String, Server> mapServers = toMapServers(servers);
		List<PhysicalServer> result = new ArrayList<>(hosts.size());

		for (TabbyHost host : hosts) {
			Server server = mapServers.get(host.name());
			if (server == null) {
				result.add(new PhysicalServer(host.name(), host.host(), host.keyPath(), false, List.of()));
			} else {
				result.add(new PhysicalServer(host.name(), host.host(), host.keyPath(), server.monitoring(), server.services()));
			}
		}

		return result;
	}

	public Map<String, Server> toMapServers(List<Server> servers) {
		Map<String, Server> result = new HashMap<>();
		servers.forEach(server -> result.put(server.name(), server));
		return result;
	}

	public Map<String, TabbyHost> toMapTabby(List<TabbyHost> hosts) {
		Map<String, TabbyHost> result = new HashMap<>();
		hosts.forEach(host -> result.put(host.name(), host));
		return result;
	}
}