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
				case "tbl-show" -> showTable();
				case "tr-show" -> showTree();
				case "sync" -> sync();
				case "sync-all" -> syncAll();
				case "scan" -> scan();
				case "tbl-scan" -> scanTable();
				case "tr-scan" -> scanTree();
				default -> System.out.println("unknown args\n" + usage());
			}
		} else {
			System.out.println("at least one arg required");
			System.out.println(usage());
			System.exit(1);
		}
	}

	private String usage() {
		return """
				usage:
					sync - quick sync
					sync-all - long sync all data
					show - show all
					tbl-show - show table
					tr-show - show tree
					scan - show with generate (for actual data)
					tbl-scan - show table with generate (for actual data)
					tr-scan - show tree with generate (for actual data)""";
	}

	/*
	 * scan dirs
	 * download tabby
	 * scan tabby
	 * print
	 */
	private void scan() {
		List<DirServer> dirServers = serversManagementService.getDirServers();
		tabbyService.downloadFileToLocal(false);
		List<TabbyServer> tabbyServers = tabbyService.parseLocalFile();
		List<Server> servers = dataService.join(dirServers, tabbyServers);

		stringFormattingService.printServersTableString(servers);
		System.out.println();
		System.out.println(stringFormattingService.servicesByServerFullTreeString(servers));
	}

	private void scanTable() {
		List<DirServer> dirServers = serversManagementService.getDirServers();
		tabbyService.downloadFileToLocal(false);
		List<TabbyServer> tabbyServers = tabbyService.parseLocalFile();
		List<Server> servers = dataService.join(dirServers, tabbyServers);

		stringFormattingService.printServersTableString(servers);
	}

	private void scanTree() {
		List<DirServer> dirServers = serversManagementService.getDirServers();
		tabbyService.downloadFileToLocal(false);
		List<TabbyServer> tabbyServers = tabbyService.parseLocalFile();
		List<Server> servers = dataService.join(dirServers, tabbyServers);

		System.out.println(stringFormattingService.servicesByServerFullTreeString(servers));
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
		tabbyService.downloadFileToLocal(true);
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
		System.out.println();
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
