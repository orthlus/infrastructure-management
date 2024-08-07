package art.aelaort;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;

import java.util.Properties;

@RequiredArgsConstructor
public class JschConnection implements AutoCloseable {
	private Session jschSession;
	private ChannelSftp channelSftp;

	private final String username;
	private final String host;
	private final int port;
	private final String privateKeyPath;

	public ChannelSftp sftp() {
		JSch jsch = jsch(privateKeyPath);
		setupSession(jsch, username, host, port);
		setupChannel();

		return channelSftp;
	}

	private JSch jsch(String privateKeyPath) {
		try {
			JSch jsch = new JSch();
			jsch.addIdentity(privateKeyPath);

			return jsch;
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
	}

	private void setupChannel() {
		try {
			channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
			channelSftp.connect();
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
	}

	private void setupSession(JSch jsch, String username, String host, int port) {
		try {
			Session session = jsch.getSession(username, host, port);
			session.setConfig(getConfig());
			session.connect();

			jschSession = session;
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
	}

	private Properties getConfig() {
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");

		return config;
	}

	@Override
	public void close() {
		channelSftp.exit();
		jschSession.disconnect();
	}
}
