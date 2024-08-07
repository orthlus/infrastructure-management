package art.aelaort;

import art.aelaort.exceptions.SshNotFountFileException;
import art.aelaort.models.TabbyServer;
import com.jcraft.jsch.SftpException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;

@Component
@RequiredArgsConstructor
public class SshClient {
	private final TabbyService tabbyService;

	public void downloadFile(Path remotePath, Path localFile, TabbyServer tabbyServer) {
		try (JschConnection jsch = jsch(tabbyServer)) {
			jsch.sftp().get(remotePath.toString(), localFile.toString());
		} catch (SftpException e) {
			throw e.id == SSH_FX_NO_SUCH_FILE
					? new SshNotFountFileException()
					: new RuntimeException(e);
		}
	}

	public void uploadFile(Path fileToUpload, Path remoteDir, TabbyServer tabbyServer) {
		try (JschConnection jsch = jsch(tabbyServer)) {
			jsch.sftp().put(
					fileToUpload.toString(),
					remoteDir.resolve(fileToUpload.getFileName()).toString());
		} catch (SftpException e) {
			throw new RuntimeException(e);
		}
	}

	private JschConnection jsch(TabbyServer tabbyServer) {
		return new JschConnection(
				"root",
				tabbyServer.host(),
				tabbyServer.port(),
				tabbyService.getKeyFullPath(tabbyServer)
		);
	}
}
