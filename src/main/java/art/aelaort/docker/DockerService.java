package art.aelaort.docker;

import art.aelaort.exceptions.DockerComposeValidationFailedException;
import art.aelaort.exceptions.NoDifferenceInFilesException;
import art.aelaort.exceptions.SshNotFountFileException;
import art.aelaort.exceptions.TooManyDockerFilesException;
import art.aelaort.models.ssh.SshServer;
import art.aelaort.ssh.SshClient;
import art.aelaort.utils.ExternalUtilities;
import art.aelaort.utils.FileDiffService;
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

import static art.aelaort.utils.Utils.linuxResolve;
import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class DockerService {
	private final SshClient sshClient;
	private final FileDiffService fileDiffService;
	private final ExternalUtilities externalUtilities;
	private final Utils utils;
	@Value("${docker.compose.remote.dir.default}")
	private String defaultRemoteDir;
	@Value("${docker.compose.remote.filename.default}")
	private String defaultRemoteFilename;
	@Value("${servers.management.dir}")
	private Path serversDir;

	public void uploadDockerFile(SshServer sshServer) {
		try {
			Path newFileLocalPath = resolveDockerFileLocalPath(sshServer.serverDirName());

			try {
				log("processing update docker compose on server '%s'%n", sshServer.serverDirName());

				validateDockerComposeFile(newFileLocalPath);
				Path oldFilePath = utils.createTmpDir().resolve(defaultRemoteFilename);
				sshClient.downloadFile(linuxResolve(defaultRemoteDir, defaultRemoteFilename), oldFilePath, sshServer);

				String coloredFilesDiff = fileDiffService.getColoredFilesDiff(oldFilePath, newFileLocalPath);
				log("new file changes:\n" + coloredFilesDiff);

				if (isApproved("\nreplace file?: ")) {
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
