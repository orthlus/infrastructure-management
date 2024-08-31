package art.aelaort;

import art.aelaort.build.BuildService;
import art.aelaort.exceptions.*;
import art.aelaort.mappers.DockerMapper;
import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.ServiceDto;
import art.aelaort.models.servers.TabbyServer;
import art.aelaort.models.ssh.SshServer;
import art.aelaort.ssh.SshClient;
import art.aelaort.utils.ExternalUtilities;
import art.aelaort.utils.Utils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Stream;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapBlue;
import static art.aelaort.utils.ColoredConsoleTextUtils.wrapGreen;
import static art.aelaort.utils.Utils.linuxResolve;
import static art.aelaort.utils.Utils.log;
import static java.util.stream.Collectors.joining;

@Component
@RequiredArgsConstructor
public class DockerService {
	private final SshClient sshClient;
	private final FileDiffService fileDiffService;
	private final ExternalUtilities externalUtilities;
	private final Utils utils;
	private final DockerMapper dockerMapper;
	private final TabbyFiles tabbyFiles;
	private final BuildService buildService;
	private final ServersManagementService serversManagementService;
	@Value("${docker.compose.remote.dir.default}")
	private String defaultRemoteDir;
	@Value("${docker.compose.remote.filename.default}")
	private String defaultRemoteFilename;
	@Value("${servers.management.dir}")
	private Path serversDir;

	public String statAllServers() {
		String statsCommand = "docker stats --no-stream --format \"table {{.Name}}\\t{{.CPUPerc}}\\t{{.MemUsage}}\\t{{.MemPerc}}\\t{{.NetIO}}\"";
		String dfhCommand = "df -h";
		String splitRow = "\n" + "=".repeat(100) + "\n";
		return serversManagementService.scanOnlyLocalData()
				.stream()
				.filter(this::hasDockerService)
				.map(dockerMapper::map)
				.map(sshServer -> prettyStdoutExecCommandsOnServer(sshServer, statsCommand, dfhCommand))
				.collect(joining(splitRow));
	}

	private String prettyStdoutExecCommandsOnServer(SshServer server, String statsCommand, String dfhCommand) {
		String statsResult = sshClient.getCommandStdout(statsCommand, server);
		String dfhResult = sshClient.getCommandStdout(dfhCommand, server);
		return """
				%s:
				%s
				%s
				
				%s
				%s
				"""
				.formatted(
						wrapGreen(server.serverDirName()),
						wrapBlue("docker stats"),
						statsResult.trim(),
						wrapBlue("df -h"),
						filterDockerStatsOutput(dfhResult).trim()
				);
	}

	private String filterDockerStatsOutput(String dockerStats) {
		return Stream.of(dockerStats.split("\n"))
				.filter(row -> !row.contains("/var/lib/docker/"))
				.collect(joining("\n"));
	}

	private boolean hasDockerService(Server server) {
		return server.getServices()
				.stream()
				.anyMatch(serviceDto -> serviceDto.getYmlName().startsWith("docker"));
	}

	public SshServer findServer(String nameOrPortOrAppNumber) {
		try {
			int appNumberOrPort = Integer.parseInt(nameOrPortOrAppNumber);
			return appNumberOrPort > 9999 ?
					getServerByPortNumber(appNumberOrPort) :
					getServerByAppNumber(appNumberOrPort);
		} catch (NumberFormatException e) {
			return getServerByName(nameOrPortOrAppNumber);
		}
	}

	private SshServer getServerByAppNumber(int appNumber) {
		String jobName = buildService.getJobsMapById().get(appNumber).getName();
		List<Server> servers = serversManagementService.scanOnlyLocalData();
		for (Server server : servers) {
			for (ServiceDto service : server.getServices()) {
				String dockerImageName = service.getDockerImageName();
				if (dockerImageName != null && dockerImageName.equals(jobName)) {
					return dockerMapper.map(server);
				}
			}
		}
		throw new ServerNotFoundException();
	}

	private SshServer getServerByPortNumber(int port) {
		List<TabbyServer> list = tabbyFiles.readLocal()
				.stream()
				.filter(s -> s.port() == port)
				.toList();
		return switch (list.size()) {
			case 0 -> throw new ServerNotFoundException();
			case 1 -> dockerMapper.map(list.get(0));
			default -> throw new ServerByPortTooManyServersException();
		};
	}

	private SshServer getServerByName(String name) {
		return tabbyFiles.readLocal()
				.stream()
				.filter(s -> s.name().equals(name))
				.map(dockerMapper::map)
				.findFirst()
				.orElseThrow(ServerNotFoundException::new);
	}

	public void uploadDockerFile(SshServer sshServer) {
		try {
			Path newFileLocalPath = resolveDockerFileLocalPath(sshServer.serverDirName());

			try {
				validateDockerComposeFile(newFileLocalPath);

				Path oldFilePath = utils.createTmpDir().resolve(defaultRemoteFilename);

				sshClient.downloadFile(linuxResolve(defaultRemoteDir, defaultRemoteFilename), oldFilePath, sshServer);

				log("processing update docker compose on server '%s'%n", sshServer.serverDirName());

				String coloredFilesDiff = fileDiffService.getColoredFilesDiff(oldFilePath, newFileLocalPath);
				log("new file changes:\n" + coloredFilesDiff);

				if (isApproved("replace file?: ")) {
					sshClient.uploadFile(newFileLocalPath, defaultRemoteDir, sshServer);
					log("new file uploaded!");
				}

				Files.deleteIfExists(oldFilePath);
			} catch (DockerComposeValidationFailedException e) {
				log("docker compose file - failed validation:%n%s", e.getStderr());
			} catch (SshNotFountFileException e) {
				log("remote file doesn't exists");
				if (isApproved("create remote file?: ")) {
					sshClient.uploadFile(newFileLocalPath, defaultRemoteDir, sshServer);
					log("remote file updated!");
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} catch (NoDifferenceInFilesException e) {
			log("new file has no changes, exit...");
		} catch (TooManyDockerFilesException e) {
			log("too many docker files found in local dir, exit...");
		}
	}

	private void validateDockerComposeFile(Path newFileLocalPath) throws DockerComposeValidationFailedException {
		Optional<String> optionalS = externalUtilities.dockerComposeValidate(newFileLocalPath);
		if (optionalS.isPresent()) {
			throw new DockerComposeValidationFailedException(optionalS.get());
		}
	}

	private boolean isApproved(String text) {
		Scanner scanner = new Scanner(System.in);
		System.out.print(text);
		String answer = scanner.nextLine()
				.replace("\n", "")
				.replace("\r", "");

		return answer.equals("y");
	}

	private Path resolveDockerFileLocalPath(String dirName) {
		Path dir = serversDir.resolve(dirName);
		Path defaultFile = dir.resolve(defaultRemoteFilename);

		if (Files.notExists(defaultFile)) {
			return lookupYmlFile(dir);
		} else {
			return defaultFile;
		}
	}

	@SneakyThrows
	private Path lookupYmlFile(Path dir) {
		List<Path> dockerFiles = Files.walk(dir, 1)
				.filter(path -> path.toFile().isFile())
				.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".yml"))
				.filter(path -> path.getFileName().toString().toLowerCase().contains("docker"))
				.toList();
		if (dockerFiles.size() == 1) {
			return dockerFiles.get(0);
		} else {
			throw new TooManyDockerFilesException();
		}
	}
}
