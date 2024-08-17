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
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import static java.lang.Integer.parseInt;

@RegisterReflectionForBinding({Job.class, Server.class})
@Component
@RequiredArgsConstructor
public class Entrypoint implements CommandLineRunner {
	private final TabbyService tabbyService;
	private final ServersManagementService serversManagementService;
	private final JoinDataService joinDataService;
	private final StringFormattingService stringFormattingService;
	private final ExternalUtilities externalUtilities;
	private final DockerService dockerService;
	private final BuildService buildService;
	private final DatabaseManageService databaseManageService;
	private final GitStatService gitStatService;
	private final ScanShowServersService scanShow;
	@Value("${docker.compose.remote.dir.default}")
	private String dockerDefaultRemoteDir;

	@Override
	public void run(String... args) {
		if (args.length >= 1) {
			switch (args[0]) {
				case "show" -> scanShow.show();
				case "tbl-show" -> scanShow.showTable();
				case "yml-show" -> scanShow.showTree();
				case "sync" -> scanShow.sync();
				case "sync-all" -> scanShow.syncAll();
				case "scan" -> scanShow.scan();
				case "tbl-scan" -> scanShow.scanTable();
				case "yml-scan" -> scanShow.scanTree();
				case "docker" -> dockerUpload(args);
				case "build" -> build(args);
				case "dblcl" -> databaseManageService.localUp();
				case "dblcl-down" -> databaseManageService.localDown();
				case "dps" -> externalUtilities.dockerPs();
				case "db-prod-status" -> databaseManageService.remoteStatus();
				case "db-prod-update" -> databaseManageService.remoteUpdate();
				case "git-stat" -> gitStat(args);
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
					show - show all (tbl and yml)
					tbl-show - show table with servers
					yml-show - show list of services from yml files
					scan - show with generate (for actual data)
					tbl-scan - show table with generate (for actual data)
					yml-scan - show tree with generate (for actual data)
					docker - upload docker-compose file by server name (by default in %s)
						server_name or port number (required)
					build - build and deploy apps
						number of app (required for run)
							without args - printing apps list
					dblcl - start local postgres and run migrations
					dblcl-down - down local postgres
					dps - alias for 'docker ps -a'
					db-prod-status - prod migrations status
					db-prod-update - execute prod migrations
					git-stat - print git stat for all local repo
						optional args: day, week, month"""
				.formatted(dockerDefaultRemoteDir);
	}

	private void gitStat(String[] args) {
		if (args.length < 2) {
			System.out.println(gitStatService.readStatForDay());
		} else {
			System.out.println(gitStatService.readStatWithInterval(args[1]));
		}
	}

	private void build(String[] args) {
		if (args.length < 2) {
			buildService.printConfig();
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
}
