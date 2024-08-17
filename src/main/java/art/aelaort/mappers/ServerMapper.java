package art.aelaort.mappers;

import art.aelaort.models.servers.DirServer;
import art.aelaort.models.servers.TabbyServer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ServerMapper {
	public Map<String, DirServer> toMapServers(List<DirServer> dirServers) {
		return dirServers.stream().collect(Collectors.toMap(DirServer::name, Function.identity()));
	}

	public Map<String, TabbyServer> toMapTabby(List<TabbyServer> tabbyServers) {
		return tabbyServers.stream().collect(Collectors.toMap(TabbyServer::name, Function.identity()));
	}
}