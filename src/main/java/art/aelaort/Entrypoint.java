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
	private final ExternalUtilities externalUtilities;

	@Override
	public void run(String... args) {
		if (args.length == 1) {
			switch (args[0]) {
				case "show" -> show();
				case "show-table" -> showTable();
				case "show-tree" -> showTree();
				case "sync" -> sync();
				case "sync-all" -> syncAll();
			}
		} else {
			System.out.println("at least one arg required");
			System.exit(1);
		}
	}

	/*
	* download tabby
	* generate json
	* save data to local
	* save data to s3
	* save ips to s3
	*/
	private void sync() {
		List<DirServer> dirServers = serversManagementService.getDirServers();
		tabbyService.downloadFileToLocal();
		List<TabbyServer> tabbyServers = tabbyService.parseLocalFile();
		List<Server> servers = dataService.join(dirServers, tabbyServers);
		serversManagementService.saveData(servers);
		serversManagementService.saveIps(servers);
		System.out.println("sync done");
	}

	private void syncAll() {
		sync();
		externalUtilities.ydSync();
	}

	/*
	* read local json
	* print table
	* print tree
	*/
	private void show() {
		List<Server> servers = serversManagementService.readLocalJsonData();
		stringFormattingService.printServersTableString(servers);
		System.out.println(stringFormattingService.servicesByServerFullTreeString(servers));
	}

	private void showTree() {
		List<Server> servers = serversManagementService.readLocalJsonData();
		System.out.println(stringFormattingService.servicesByServerFullTreeString(servers));
	}

	private void showTable() {
		List<Server> servers = serversManagementService.readLocalJsonData();
		stringFormattingService.printServersTableString(servers);
	}
}
