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
import art.aelaort.ssh.SshKeyCloudUploader;
import art.aelaort.ssh.SshKeyGenerator;
import art.aelaort.ssh.SshKeyUploader;
import art.aelaort.ssh.SshKeysCleanupService;
import art.aelaort.utils.ExternalUtilities;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import static art.aelaort.utils.Utils.log;
import static art.aelaort.utils.Utils.slice;
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
	private final SshKeyGenerator sshKeyGenerator;
	private final SshKeyCloudUploader sshKeyCloudUploader;
	@Value("${docker.compose.remote.dir.default}")
	private String dockerDefaultRemoteDir;

	@Override
	public void run(String... args) {
		if (args.length >= 1) {
			switch (args[0]) {
				case "show" -> 				 scanShow.show();
				case "tbl" ->				 scanShow.showTable();
				case "yml", "svc" ->		 scanShow.showYml();
				case "sync", "s" -> 		 scanShow.sync();
				case "sync-all", "sa" -> 	 scanShow.syncAll();
				case "docker" -> 			 dockerUpload(args);
				case "build" -> 			 build(args);
				case "build-ls-type" ->		 buildListByType(args);
				case "dbl" -> 				 localDb.localUpFromEntry(args);
				case "dbl-down", "dbld" -> 	 localDb.localDown();
				case "dbl-rerun-jooq",
					 "dblrrj" -> 			 localDb.localRerunAndGenJooq(args);
				case "dbp-status", "dbps" -> remoteDb.remoteStatus(args);
				case "dbp-run", "dbpr" -> 	 remoteDb.remoteRun(args);
				case "dps" -> 				 externalUtilities.dockerPs();
				case "git-stat" -> 			 gitStat(args);
				case "host-stat", "hs" ->	 hostStats(args);
				case "make" -> 				 makeProject(args);
				case "upld-ssh" -> 			 uploadSshKey(args);
				case "clean-ssh" -> 		 sshKeysCleanupService.cleanAll();
				case "gen-ssh", "gen" ->     sshKeyGenerator.generateKey(slice(args, 1));
				case "gen-ssh-upload",
					 "gsu" ->                genSshUpload(args);
				case "port" -> 				 log(randomPortService.getRandomPort());
				case "portk" ->				 log(randomPortService.getRandomPortK8s());
				case "vm" -> 				 virtualBoxService.up(args);
				case "vm-pause", "vmp" -> 	 virtualBoxService.pause(args);
				case "vm-stop", "vms" -> 	 virtualBoxService.stop(args);
				case "vml" -> 				 virtualBoxService.vml();
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
					tbl            - show table with servers
					yml, svc       - show list of services from yml files
					\s
					docker - upload docker-compose file to server (in %s)
					            by server id/name (required)
					\s
					build - build and deploy apps
					            number of app (required for run)
					                without args - printing apps list
					build-ls-type - apps list by build type alias
					                no args print all (also with deprecated)
					\s
					Databases (optional 1 arg - db name):
					dbl                 - start local postgres and run migrations
					dbl-down, dbld      - down local postgres
					dbl-rerun-jooq,     - local down and up, if passed app id - run jooq
					dblrrj
					dbp-status, dbps    - prod migrations status
					dbp-run, dbpr       - execute prod migrations
					\s
					dps         - alias for 'docker ps -a'
					git-stat    - print git stat for all local repo
					                optional args: day, week, month
					host-stat   - remote system stats (docker and host)
					hs              by server id/name
					                or all servers if no arguments
					make         - create project folder
					                 one arg required
					                 `name` for make project by name (could be with sub directories)
					                     or
					                 `id` for make project with name from config by id
					                 optional:
					                     `no-git` - not init git
					                     `jooq` - add jooq config and plugin
					upld-ssh     - upload ssh key to server
					                 by server id/name
					                 required key file path (2 arg)
					                 required user name (3 arg)
					clean-ssh    - clean unused ssh keys (local and in timeweb)
					gen-ssh, gen - generate local ssh key
					                 1 arg - name
					                 2 arg (optional) - comment
					gen-ssh-upload, gsu
					             - generate ssh key and upload
					                 1 arg - name
					                 2 arg (optional) - comment
					port         - generate random, not used, port for tcp. 5 digits
					portk        - generate port for k8s (30001, 32767), not used.
					\s
					VirtualBox:
					vm              - start virtualbox
					vm-pause, vmp   - save state virtualbox
					vm-stop, vms    - shutdown virtualbox
					vml             - list all machines and running machines"""
				.formatted(dockerDefaultRemoteDir);
	}

	private void genSshUpload(String[] args) {
		String[] sliced = slice(args, 1);
		sshKeyGenerator.generateKey(sliced);
		try {
			sshKeyCloudUploader.uploadKey(sliced);
		} catch (Exception e) {
			log("error when upload: %s", e);
		}
	}

	private void hostStats(String[] args) {
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

	private void buildListByType(String[] args) {
		if (args.length > 1) {
			buildService.printConfig(args[1]);
		} else {
			buildService.printConfigWithDeprecated();
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
