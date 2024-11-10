package art.aelaort.servers.providers;

import art.aelaort.build.JobsProvider;
import art.aelaort.exceptions.ServerByPortTooManyServersException;
import art.aelaort.exceptions.ServerNotFoundException;
import art.aelaort.mappers.ServerMapper;
import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.ServiceDto;
import art.aelaort.models.servers.TabbyServer;
import art.aelaort.models.ssh.SshServer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SshServerProvider {
	private final TabbyServerProvider tabbyServerProvider;
	private final ServerMapper serverMapper;
	private final ServerProvider serverProvider;
	private final JobsProvider jobsProvider;

	public SshServer findServer(String serverNameOrServerId) {
		try {
			int serverId = Integer.parseInt(serverNameOrServerId);
			return getServerByServerId(serverId);
		} catch (NumberFormatException e) {
			return getServerByName(serverNameOrServerId);
		}
	}

	private SshServer getServerByServerId(int serverId) {
		return serverProvider.scanOnlyLocalData().stream()
				.filter(server -> server.getId().equals(serverId))
				.map(serverMapper::map)
				.findFirst()
				.orElseThrow(ServerNotFoundException::new);
	}

	private SshServer getServerByAppNumber(int appNumber) {
		String jobName = jobsProvider.getJobsMapById().get(appNumber).getName();
		List<Server> servers = serverProvider.scanOnlyLocalData();
		for (Server server : servers) {
			for (ServiceDto service : server.getServices()) {
				String dockerImageName = service.getDockerImageName();
				if (dockerImageName != null && dockerImageName.equals(jobName)) {
					return serverMapper.map(server);
				}
			}
		}
		throw new ServerNotFoundException();
	}

	private SshServer getServerByPortNumber(int port) {
		List<TabbyServer> list = tabbyServerProvider.readLocal()
				.stream()
				.filter(s -> s.port() == port)
				.toList();
		return switch (list.size()) {
			case 0 -> throw new ServerNotFoundException();
			case 1 -> serverMapper.map(list.get(0));
			default -> throw new ServerByPortTooManyServersException();
		};
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
