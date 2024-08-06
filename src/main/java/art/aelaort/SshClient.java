package art.aelaort;

import art.aelaort.models.TabbyServer;
import com.jcraft.jsch.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class SshClient {
	private final TabbyService tabbyService;

	public void downloadFile(String remotePath, Path localDir, TabbyServer tabbyServer) {

	}

	public void uploadFile(Path fileToUpload, TabbyServer tabbyServer) {
		uploadFile(fileToUpload, "/root", tabbyServer);
	}

	public void uploadFile(Path fileToUpload, String remoteDir, TabbyServer tabbyServer) {
		try {
			ChannelSftp channelSftp = setupJsch(tabbyServer);
			channelSftp.connect();

			String localFile = fileToUpload.toString();
			String dir = remoteDir.endsWith("/") ? remoteDir : remoteDir + "/";

			channelSftp.put(localFile, dir + fileToUpload.getFileName().toString());

			channelSftp.exit();
		} catch (JSchException | SftpException e) {
			throw new RuntimeException(e);
		}
	}

	private ChannelSftp setupJsch(TabbyServer tabbyServer) {
		return setupJsch(
				"root",
				tabbyServer.host(),
				tabbyServer.port(),
				tabbyService.getKeyFullPath(tabbyServer)
		);
	}

	private ChannelSftp setupJsch(String username, String host, int port, String privateKeyPath) {
		try {
			JSch jsch = new JSch();
	//		jsch.setKnownHosts("/Users/john/.ssh/known_hosts");
			jsch.addIdentity(privateKeyPath);
			Session jschSession = jsch.getSession(username, host, port);
			jschSession.connect();
			return (ChannelSftp) jschSession.openChannel("sftp");
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
	}
}
