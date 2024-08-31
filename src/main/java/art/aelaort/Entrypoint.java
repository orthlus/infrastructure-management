package art.aelaort;

import art.aelaort.build.BuildService;
import art.aelaort.exceptions.*;
import art.aelaort.make.ProjectsMakerService;
import art.aelaort.models.build.Job;
import art.aelaort.models.ssh.SshServer;
import art.aelaort.scan_show.ScanShowServersService;
import art.aelaort.utils.ExternalUtilities;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import static art.aelaort.utils.Utils.log;
import static java.lang.Integer.parseInt;

@Component
@RequiredArgsConstructor
public class Entrypoint implements CommandLineRunner {
	private final ExternalUtilities externalUtilities;
	private final DockerService dockerService;
	private final BuildService buildService;
	private final DatabaseManageService databaseManageService;
	private final GitStatService gitStatService;
	private final ScanShowServersService scanShow;
	private final ProjectsMakerService projectsMakerService;
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
				case "proxy" -> externalUtilities.proxyUp();
				case "proxy-d" -> externalUtilities.proxyDown();
				case "dstat" -> log(dockerService.statAllServers());
				case "jmake" -> makeProject(args);
				default -> log("unknown args\n" + usage());
			}
		} else {
			log("at least one arg required");
			log(usage());
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
						optional args: day, week, month
					proxy - start socks5 proxy
					proxy-d - stop socks5 proxy
					dstat - docker stats and df -h from all servers
					jmake - create project folder
						one arg required
						`name` for make project by name (could be with sub directories)
							or
						`id` for make project with name from config by id
						optional:
							`no-git` - not init git
							`jooq` - add jooq config and plugin"""
				.formatted(dockerDefaultRemoteDir);
	}

	private void makeProject(String[] args) {
		if (args.length < 2) {
			log("project `name` or `id` required");
			log(usage());
			System.exit(1);
		} else {
			String nameOrId = args[1];
			try {
				boolean hasGit = projectsMakerService.hasGit(args);
				boolean hasJooq = projectsMakerService.hasJooq(args);
				projectsMakerService.makeJavaProject(nameOrId, hasGit, hasJooq);
			} catch (InvalidAppParamsException e) {
				log("make project by id - app found, but params not correct");
			} catch (AppNotFoundException e) {
				log("app by id %d not found\n", e.getProject().getId());
			} catch (ProjectAlreadyExistsException e) {
				log("project create failed - dir %s already exists\n", e.getDir());
			}
		}
	}

	private void gitStat(String[] args) {
		if (args.length < 2) {
			log(gitStatService.readStatForDay());
		} else {
			log(gitStatService.readStatWithInterval(args[1]));
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
				log("too many docker-files");
			} catch (BuildJobNotFoundException e) {
				log("job %s not found\n", args[1]);
			}
		}
	}

	private void dockerUpload(String[] args) {
		if (args.length >= 2) {
			try {
				SshServer sshServer = dockerService.findServer(args[1]);
				dockerService.uploadDockerFile(sshServer);
			} catch (ServerNotFoundException e) {
				log("server not found");
			} catch (ServerByPortTooManyServersException e) {
				log("too many servers found, need more uniq param or fix data");
			}
		} else {
			log("at least 2 args required");
			log(usage());
			System.exit(1);
		}
	}
}
