package art.aelaort;

import art.aelaort.exceptions.BuildJobNotFoundException;
import art.aelaort.exceptions.TabbyServerByPortTooManyServersException;
import art.aelaort.exceptions.TabbyServerNotFoundException;
import art.aelaort.exceptions.TooManyDockerFilesException;
import art.aelaort.models.build.Job;
import art.aelaort.models.servers.DirServer;
import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.TabbyServer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.Integer.parseInt;

@Component
@RequiredArgsConstructor
public class Entrypoint implements CommandLineRunner {
	private final TabbyService tabbyService;
	private final ServersManagementService serversManagementService;
	private final DataService dataService;
	private final StringFormattingService stringFormattingService;
	private final ExternalUtilities externalUtilities;
	private final DockerService dockerService;
	private final BuildService buildService;
	@Value("${docker.compose.remote.dir.default}")
	private String dockerDefaultRemoteDir;

	@Override
	public void run(String... args) {
		if (args.length >= 1) {
			switch (args[0]) {
				case "show" -> show();
				case "tbl-show" -> showTable();
				case "yml-show" -> showTree();
				case "sync" -> sync();
				case "sync-all" -> syncAll();
				case "scan" -> scan();
				case "tbl-scan" -> scanTable();
				case "yml-scan" -> scanTree();
				case "docker" -> dockerUpload(args);
				case "build" -> build(args);
				default -> System.out.println("unknown args\n" + usage());
			}
		} else {
			System.out.println("at least one arg required");
			System.out.println(usage());
			System.exit(1);
		}
	}

	public void build(String[] args) {
		if (args.length < 2) {
			buildService.printConfig();
			System.exit(0);
		} else {
			try {
				Job job = buildService.getJobById(parseInt(args[1]));
				boolean isBuildDockerNoCache = buildService.isBuildDockerNoCache(args);
				buildService.run(job, isBuildDockerNoCache);
			} catch (TooManyDockerFilesException e) {
				System.out.println("too many docker-files");
			} catch (BuildJobNotFoundException e) {
				System.out.printf("job %s not found\n", args[1]);
			}
		}
	}

	private void dockerUpload(String[] args) {
		if (args.length >= 2) {
			try {
				TabbyServer server = tabbyService.findTabbyServer(args[1]);
				dockerService.uploadDockerFile(server);
			} catch (TabbyServerNotFoundException e) {
				System.out.println("server don't found");
			} catch (TabbyServerByPortTooManyServersException e) {
				System.out.println("too many servers found, need more uniq param or fix data");
			}
		} else {
			System.out.println("at least 2 args required");
			System.out.println(usage());
			System.exit(1);
		}
	}

	private String usage() {
		return """
				usage:
					sync - quick sync
					sync-all - long sync all data
					show - show all (tbl and yml)
					tbl-show - show table with servers
					yml-show - show list of services from yml files
					scan - show with generate (for actual data)
					tbl-scan - show table with generate (for actual data)
					yml-scan - show tree with generate (for actual data)
					docker - upload docker-compose file by server name (by default in %s)
						server_name or port number (required)""".formatted(dockerDefaultRemoteDir);
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
