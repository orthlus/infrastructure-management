package art.aelaort;

import art.aelaort.docker.DockerService;
import art.aelaort.docker.HostStatsService;
import art.aelaort.exceptions.*;
import art.aelaort.k8s.K8sApplyService;
import art.aelaort.make.ProjectsMakerService;
import art.aelaort.models.ssh.SshServer;
import art.aelaort.scan_show.ScanShowServersService;
import art.aelaort.servers.providers.SshServerProvider;
import art.aelaort.ssh.SshKeyCloudUploader;
import art.aelaort.ssh.SshKeyGenerator;
import art.aelaort.ssh.SshKeyUploader;
import art.aelaort.ssh.SshKeysCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import static art.aelaort.utils.Utils.log;
import static art.aelaort.utils.Utils.slice;

@Component
@RequiredArgsConstructor
public class Entrypoint implements CommandLineRunner {
	private final DockerService dockerService;
	private final ScanShowServersService scanShow;
	private final ProjectsMakerService projectsMakerService;
	private final HostStatsService hostStatsService;
	private final SshKeyUploader sshKeyUploader;
	private final SshServerProvider sshServerProvider;
	private final SshKeysCleanupService sshKeysCleanupService;
	private final SshKeyGenerator sshKeyGenerator;
	private final SshKeyCloudUploader sshKeyCloudUploader;
	private final K8sApplyService k8SApplyService;
	@Value("${docker.compose.remote.dir.default}")
	private String dockerDefaultRemoteDir;

	@Override
	public void run(String... args) {
		if (args.length >= 1) {
			switch (args[0]) {
				case "show" -> 				 scanShow.show();
				case "tbl" ->				 scanShow.showTable();
				case "yml", "svc" ->		 scanShow.showYml();
				case "k8s-list", "kl" ->	 scanShow.showK8s();
				case "docker" -> 			 dockerUpload(args);
				case "kub", "k" -> 			 k8SApplyService.apply(slice(args, 1));
				case "k8s-docker-login" ->   k8SApplyService.printDockerConfigJson(slice(args, 1));
				case "host-stat", "hs" ->	 hostStats(args);
				case "make" -> 				 makeProject(args);
				case "upld-ssh" -> 			 uploadSshKey(args);
				case "clean-ssh" -> 		 sshKeysCleanupService.cleanAll();
				case "gen-ssh", "gen" ->     sshKeyGenerator.generateKey(slice(args, 1));
				case "gen-ssh-upload",
					 "gsu" ->                genSshUpload(args);
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
					show           - show all (tbl and yml)
					tbl            - show table with servers
					yml, svc       - show list of services from yml files
					\s
					docker - upload docker-compose file to server (in %s)
					            by server id/name (required)
					\s
					kub, k              - apply yaml for cluster.
					                      1. yaml file name is optional (by default apply all files)
					                      2. cluster name is optional (default name in file)
					k8s-docker-login    - generate base64 secret with docker login info for k8s
					                      args: registry, login, password
					                      or string in docker login format
					\s
					host-stat   - remote system stats (docker and host)
					hs              by server id/name
					                or all servers if no arguments
					make         - create project folder
					               required: `id` for make project with name from config by id
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
					                 2 arg (optional) - comment"""
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
				log(hostStatsService.statByServer(sshServer));
			} catch (ServerNotFoundException e) {
				log("server not found");
			} catch (ServerByPortTooManyServersException e) {
				log("too many servers found, need more uniq param or fix data");
			}
		} else {
			log(hostStatsService.statAllServers());
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
				projectsMakerService.makeJavaProject(nameOrId);
			} catch (InvalidAppParamsException e) {
				log("make project by id - app found, but params not correct");
			} catch (AppNotFoundException e) {
				log("app by id %d not found\n", e.getProject().getId());
			} catch (ProjectAlreadyExistsException e) {
				log("project create failed - dir %s already exists\n", e.getDir());
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
