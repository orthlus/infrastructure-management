package art.aelaort.ssh;

import art.aelaort.exceptions.SshNotFountFileException;
import art.aelaort.models.ssh.SshServer;
import com.jcraft.jsch.SftpException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

import static art.aelaort.utils.Utils.linuxResolve;
import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;

@Component
@RequiredArgsConstructor
public class SshClient {
	public void downloadFile(String remotePath, Path localFile, SshServer server) {
		try (JschConnection jsch = jsch(server)) {
			jsch.sftp().get(remotePath, localFile.toString());
		} catch (SftpException e) {
			throw e.id == SSH_FX_NO_SUCH_FILE
					? new SshNotFountFileException()
					: new RuntimeException(e);
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