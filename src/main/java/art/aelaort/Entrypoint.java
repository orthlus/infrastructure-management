package art.aelaort;

import art.aelaort.models.DirServer;
import art.aelaort.models.PhysicalServer;
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
		List<PhysicalServer> physicalServers = dataService.join(dirServers, tabbyServers);
		String json = serializeService.toJson(physicalServers);
		System.out.println(json);
		List<PhysicalServer> physicalServers1 = serializeService.serversParse(json);
		System.out.println(stringFormattingService.serversTableString(physicalServers1));
//		System.out.println(stringFormattingService.servicesByServerFullTreeString(physicalServers));
	}
}
