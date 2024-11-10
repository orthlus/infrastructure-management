package art.aelaort.ssh;

import art.aelaort.exceptions.SshNotFountFileException;
import art.aelaort.models.ssh.SshServer;
import com.jcraft.jsch.SftpException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static art.aelaort.utils.Utils.linuxResolve;
import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;

@Slf4j
@Component
@RequiredArgsConstructor
public class SshClient {
	public void execCommandInheritIO(String command, SshServer server) {
		log.debug("ssh - executing '{}' on server {}", command, server.host());
		try (JschConnection jsch = jsch(server)) {
			InputStream is = jsch.exec(command);
			IOUtils.copy(is, System.out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String getCommandStdout(String command, SshServer server) {
		try (JschConnection jsch = jsch(server)) {
			InputStream is = jsch.exec(command);

			return IOUtils.toString(is, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void downloadFile(String remotePath, Path localFile, SshServer server) {
		try (JschConnection jsch = jsch(server)) {
			jsch.sftp().get(remotePath, localFile.toString());
		} catch (SftpException e) {
			throw e.id == SSH_FX_NO_SUCH_FILE
					? new SshNotFountFileException()
					: new RuntimeException(e);
		}
	}

	public void mkdir(String remotePath, SshServer server) {
		try (JschConnection jsch = jsch(server)) {
			jsch.sftp().mkdir(remotePath);
		} catch (Exception ignored) {
		}
	}

	public void uploadFileNewName(Path fileToUpload, String remotePath, SshServer server) {
		try (JschConnection jsch = jsch(server)) {
			jsch.sftp().put(
					fileToUpload.toString(),
					remotePath);
		} catch (SftpException e) {
			throw new RuntimeException(e);
		}
	}

	public void uploadFile(Path fileToUpload, String remoteDir, SshServer server) {
		try (JschConnection jsch = jsch(server)) {
			jsch.sftp().put(
					fileToUpload.toString(),
					linuxResolve(remoteDir, fileToUpload.getFileName()));
		} catch (SftpException e) {
			throw new RuntimeException(e);
		}
	}

	private JschConnection jsch(SshServer server) {
		return new JschConnection("root", server);
	}
}
