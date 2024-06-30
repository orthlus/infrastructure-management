package art.aelaort;

import art.aelaort.models.DirServer;
import art.aelaort.models.Server;
import art.aelaort.models.TabbyServer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class Entrypoint implements CommandLineRunner {
	private final TabbyService tabbyService;
	private final ServersManagementService serversManagementService;
	private final DataService dataService;
	private final StringFormattingService stringFormattingService;
	private final SerializeService serializeService;

	@Override
	public void run(String... args) {
		System.out.println(args.length);
		if (args.length == 1) {
			switch (args[0]) {
				case "show" -> show();
				case "sync" -> sync();
			}
		} else {
			System.out.println("at least one arg required");
			System.exit(1);
		}
	}

	private void sync() {
		List<DirServer> dirServers = serversManagementService.getDirServers();
		List<TabbyServer> tabbyServers = tabbyService.parseLocalFile();
		List<Server> servers = dataService.join(dirServers, tabbyServers);
		String json = serializeService.toJson(servers);
		serversManagementService.saveJsonToLocal(json);
		// generate json - done
		// save to local - done
		// upload json to s3
	}

	private void show() {
		String jsonStr = serversManagementService.readJsonDataLocal();
		List<Server> servers = serializeService.serversParse(jsonStr);
		System.out.println(stringFormattingService.serversTableString(servers));
		System.out.println();
		System.out.println(stringFormattingService.servicesByServerFullTreeString(servers));
		// read local json - done
		// print table and tree - done
	}
}
