package art.aelaort;

import art.aelaort.models.PhysicalServer;
import art.aelaort.models.Server;
import art.aelaort.models.TabbyHost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class Entrypoint implements CommandLineRunner {
	private final TabbyService tabbyService;
	private final ServersManagementService serversManagementService;
	private final DataService dataService;
	private final StringFormattingService stringFormattingService;

	@Override
	public void run(String... args) throws Exception {
		List<Server> servers = serversManagementService.getServers();
		List<TabbyHost> tabbyHosts = tabbyService.parseLocalFile();
		List<PhysicalServer> physicalServers = dataService.join(servers, tabbyHosts);
		System.out.println(stringFormattingService.serversTableString(physicalServers));
		System.out.println();
//		System.out.println(stringFormattingService.servicesByServerFullTreeString(physicalServers));
	}
}
