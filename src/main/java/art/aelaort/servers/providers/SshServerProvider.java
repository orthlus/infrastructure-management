package art.aelaort.servers.providers;

import art.aelaort.exceptions.ServerNotFoundException;
import art.aelaort.mappers.ServerMapper;
import art.aelaort.models.ssh.SshServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SshServerProvider {
	private final TabbyServerProvider tabbyServerProvider;
	private final ServerMapper serverMapper;
	private final ServerProvider serverProvider;

	public SshServer findServer(String serverNameOrServerId) {
		try {
			int serverId = Integer.parseInt(serverNameOrServerId);
			return getServerByServerId(serverId);
		} catch (NumberFormatException e) {
			return getServerByName(serverNameOrServerId);
		}
	}

	private SshServer getServerByServerId(int serverId) {
		return serverProvider.readLocalJsonData().stream()
				.filter(server -> server.getId().equals(serverId))
				.map(serverMapper::map)
				.findFirst()
				.orElseThrow(ServerNotFoundException::new);
	}

	private SshServer getServerByName(String name) {
		return tabbyServerProvider.readLocal()
				.stream()
				.filter(s -> s.name().equals(name))
				.map(serverMapper::map)
				.findFirst()
				.orElseThrow(ServerNotFoundException::new);
	}
}
