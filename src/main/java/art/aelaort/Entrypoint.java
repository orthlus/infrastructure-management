package art.aelaort;

import art.aelaort.build.BuildService;
import art.aelaort.build.JobsProvider;
import art.aelaort.db.LocalDb;
import art.aelaort.db.RemoteDb;
import art.aelaort.docker.DockerService;
import art.aelaort.docker.DockerStatsService;
import art.aelaort.exceptions.*;
import art.aelaort.make.ProjectsMakerService;
import art.aelaort.models.build.Job;
import art.aelaort.models.ssh.SshServer;
import art.aelaort.scan_show.ScanShowServersService;
import art.aelaort.servers.providers.SshServerProvider;
import art.aelaort.ssh.SshKeyUploader;
import art.aelaort.ssh.SshKeysCleanupService;
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
	private final GitStatService gitStatService;
	private final ScanShowServersService scanShow;
	private final ProjectsMakerService projectsMakerService;
	private final DockerStatsService dockerStatsService;
	private final SshKeyUploader sshKeyUploader;
	private final RandomPortService randomPortService;
	private final SshServerProvider sshServerProvider;
	private final JobsProvider jobsProvider;
	private final VirtualBoxService virtualBoxService;
	private final LocalDb localDb;
	private final RemoteDb remoteDb;
	private final SshKeysCleanupService sshKeysCleanupService;
	@Value("${docker.compose.remote.dir.default}")
	private String dockerDefaultRemoteDir;

	@Override
	public void run(String... args) {
		if (args.length >= 1) {
			switch (args[0]) {
				case "show" -> 				scanShow.show();
				case "tbl-show", "tbl" -> 	scanShow.showTable();
				case "yml-show", "yml" -> 	scanShow.showYml();
				case "sync", "s" -> 		scanShow.sync();
				case "sync-all", "sa" -> 	scanShow.syncAll();
				case "scan" -> 				scanShow.scan();
				case "tbl-scan" -> 			scanShow.scanTable();
				case "yml-scan" -> 			scanShow.scanYml();
				case "docker" -> 			dockerUpload(args);
				case "build" -> 			build(args);
				case "dbl" -> 				localDb.localUp(args);
				case "dbl-down", "dbld" -> 	localDb.localDown();
				case "dbp-status", "dbps" -> remoteDb.remoteStatus(args);
				case "dbp-run", "dbpr" -> 	remoteDb.remoteRun(args);
				case "dps" -> 				externalUtilities.dockerPs();
				case "git-stat" -> 			gitStat(args);
				case "dstat", "ds" -> 		dockerStats(args);
				case "make" -> 				makeProject(args);
				case "upld-ssh" -> 			uploadSshKey(args);
				case "clean-ssh" -> 		sshKeysCleanupService.cleanAll();
				case "port" -> 				log(randomPortService.getRandomPort());
				case "vm" -> 				virtualBoxService.up(args);
				case "vm-pause", "vmp" -> 	virtualBoxService.pause(args);
				case "vm-stop", "vms" -> 	virtualBoxService.stop(args);
				case "vml" -> 				virtualBoxService.vml();
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
					sync, s        - quick sync
					sync-all, sa   - long sync all data
					show           - show all (tbl and yml)
					tbl-show, tbl  - show table with servers
					yml-show, yml  - show list of services from yml files
					scan           - show with generate (for actual data)
					tbl-scan       - show table with generate (for actual data)
					yml-scan       - show tree with generate (for actual data)
					\s
					docker - upload docker-compose file to server (in %s)
					            by server id/name (required)
					\s
					build - build and deploy apps
					            number of app (required for run)
					                without args - printing apps list
					\s
					Databases (optional 1 arg - db name):
					dbl                 - start local postgres and run migrations
					dbl-down, dbld      - down local postgres
					dbp-status, dbps    - prod migrations status
					dbp-run, dbpr       - execute prod migrations
					\s
					dps         - alias for 'docker ps -a'
					git-stat    - print git stat for all local repo
					                optional args: day, week, month
					dstat, ds   - docker stats, docker ps -a and df -h
					                by server id/name
					                or all servers if no arguments
					make        - create project folder
					                one arg required
					                `name` for make project by name (could be with sub directories)
					                    or
					                `id` for make project with name from config by id
					                optional:
					                    `no-git` - not init git
					                    `jooq` - add jooq config and plugin
					upld-ssh    - upload ssh key to server
					                by server id/name
					                required key file path (2 arg)
					                required user name (3 arg)
					clean-ssh   - clean unused ssh keys (local and in timeweb)
					port        - generate random, not used, port for tcp. 5 digits
					\s
					VirtualBox:
					vm              - start virtualbox
					vm-pause, vmp   - save state virtualbox
					vm-stop, vms    - shutdown virtualbox
					vml             - list all machines and running machines"""
				.formatted(dockerDefaultRemoteDir);
	}

	private void dockerStats(String[] args) {
		if (args.length >= 2) {
			try {
				SshServer sshServer = sshServerProvider.findServer(args[1]);
				log(dockerStatsService.statByServer(sshServer));
			} catch (ServerNotFoundException e) {
				log("server not found");
			} catch (ServerByPortTooManyServersException e) {
				log("too many servers found, need more uniq param or fix data");
			}
		} else {
			log(dockerStatsService.statAllServers());
		}
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
				Job job = jobsProvider.getJobById(parseInt(args[1]));
				boolean isBuildDockerNoCache = buildService.isBuildDockerNoCache(args);
				buildService.run(job, isBuildDockerNoCache);
			} catch (TooManyDockerFilesException e) {
				log("too many docker-files");
			} catch (BuildJobNotFoundException e) {
				log("job %s not found\n", args[1]);
			}
		}
	}

	private void uploadSshKey(String[] args) {
		validArgs(args, 4);

		try {
			SshServer sshServer = sshServerProvider.findServer(args[1]);
			sshKeyUploader.uploadSshKey(sshServer, args[2], args[3]);
		} catch (LocalFileNotFountException e) {
			log("file to upload not found: %s%n", e.getMessage());
		} catch (ServerNotFoundException e) {
			log("server not found");
		} catch (ServerByPortTooManyServersException e) {
			log("too many servers found, need more uniq param or fix data");
		}
	}

	private void dockerUpload(String[] args) {
		validArgs(args, 2);

		try {
			SshServer sshServer = sshServerProvider.findServer(args[1]);
			dockerService.uploadDockerFile(sshServer);
		} catch (ServerNotFoundException e) {
			log("server not found");
		} catch (ServerByPortTooManyServersException e) {
			log("too many servers found, need more uniq param or fix data");
		}
	}

	private void validArgs(String[] args, int requiredLength) {
		if (args.length < requiredLength) {
			log("at least %d args required%n", requiredLength);
			log(usage());
			System.exit(1);
		}
	}
}
