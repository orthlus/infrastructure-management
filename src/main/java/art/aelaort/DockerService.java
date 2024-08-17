package art.aelaort;

import art.aelaort.exceptions.DockerComposeValidationFailedException;
import art.aelaort.exceptions.NoDifferenceInFilesException;
import art.aelaort.exceptions.SshNotFountFileException;
import art.aelaort.exceptions.TooManyDockerFilesException;
import art.aelaort.mappers.DockerMapper;
import art.aelaort.models.servers.TabbyServer;
import art.aelaort.models.ssh.SshServer;
import art.aelaort.system.Response;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static art.aelaort.Utils.linuxResolve;
import static java.nio.file.Path.of;

@Component
@RequiredArgsConstructor
public class DockerService {
	private final SshClient sshClient;
	private final FileDiffService fileDiffService;
	private final ExternalUtilities externalUtilities;
	private final Utils utils;
	private final DockerMapper dockerMapper;
	@Value("${docker.compose.remote.dir.default}")
	private String defaultRemoteDir;
	@Value("${docker.compose.remote.filename.default}")
	private String defaultRemoteFilename;
	@Value("${servers.management.dir}")
	private String serversDir;

	public void uploadDockerFile(TabbyServer server) {
		try {
			Path newFileLocalPath = resolveDockerFileLocalPath(sshServer.serverDirName());

			try {
				validateDockerComposeFile(newFileLocalPath);

				Path oldFilePath = utils.createTmpDir().resolve(defaultRemoteFilename);

				sshClient.downloadFile(linuxResolve(defaultRemoteDir, defaultRemoteFilename), oldFilePath, sshServer);

				System.out.printf("processing update docker compose on server '%s'%n", sshServer.serverDirName());

				String coloredFilesDiff = fileDiffService.getColoredFilesDiff(oldFilePath, newFileLocalPath);
				System.out.println("new file changes:\n" + coloredFilesDiff);

				if (isApproved("replace file?: ")) {
					sshClient.uploadFile(newFileLocalPath, defaultRemoteDir, sshServer);
					System.out.println("new file uploaded!");
				}

				Files.deleteIfExists(oldFilePath);
			} catch (DockerComposeValidationFailedException e) {
				Response r = e.getResponse();
				System.out.printf("docker compose file - failed validation:%n%s", r.stderr());
			} catch (SshNotFountFileException e) {
				System.out.println("remote file doesn't exists");
				if (isApproved("create remote file?: ")) {
					sshClient.uploadFile(newFileLocalPath, defaultRemoteDir, sshServer);
					System.out.println("remote file updated!");
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} catch (NoDifferenceInFilesException e) {
			System.out.println("new file has no changes, exit...");
		} catch (TooManyDockerFilesException e) {
			System.out.println("too many docker files found in local dir, exit...");
		}
	}

	private void validateDockerComposeFile(Path newFileLocalPath) throws DockerComposeValidationFailedException {
		externalUtilities.dockerComposeValidate(newFileLocalPath);
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
		Path dir = of(serversDir).resolve(dirName);
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
