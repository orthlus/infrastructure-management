package art.aelaort.servers.providers;

import art.aelaort.build.BuildService;
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
	private final BuildService buildService;
	private final ServerProvider serverProvider;

	public SshServer findServer(String nameOrPortOrAppNumber) {
		try {
			int appNumberOrPort = Integer.parseInt(nameOrPortOrAppNumber);
			return appNumberOrPort > 9999 ?
					getServerByPortNumber(appNumberOrPort) :
					getServerByAppNumber(appNumberOrPort);
		} catch (NumberFormatException e) {
			return getServerByName(nameOrPortOrAppNumber);
		}
	}

	private SshServer getServerByAppNumber(int appNumber) {
		String jobName = buildService.getJobsMapById().get(appNumber).getName();
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
