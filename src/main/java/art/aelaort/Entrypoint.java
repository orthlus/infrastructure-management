package art.aelaort;

import art.aelaort.models.DirServer;
import art.aelaort.models.Server;
import art.aelaort.models.TabbyServer;
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
	private final SerializeService serializeService;

	@Override
	public void run(String... args) throws Exception {
		List<DirServer> dirServers = serversManagementService.getDirServers();
		List<TabbyServer> tabbyServers = tabbyService.parseLocalFile();
		List<Server> servers = dataService.join(dirServers, tabbyServers);
		String json = serializeService.toJson(servers);
		System.out.println(json);
		List<Server> servers1 = serializeService.serversParse(json);
		System.out.println(stringFormattingService.serversTableString(servers1));
//		System.out.println(stringFormattingService.servicesByServerFullTreeString(physicalServers));
	}
}
