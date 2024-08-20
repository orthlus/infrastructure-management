package art.aelaort.ssh;

import art.aelaort.models.ssh.SshServer;
import com.jcraft.jsch.*;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@RequiredArgsConstructor
public class JschConnection implements AutoCloseable {
	private Session jschSession;
	private ChannelSftp channelSftp;
	private ChannelExec channelExec;

	private final String username;
	private final String host;
	private final int port;
	private final String privateKeyPath;

	public JschConnection(String username, SshServer server) {
		this(username, server.host(), server.port(), server.fullKeyPath());
	}

	public InputStream exec(String command) {
		try {
			JSch jsch = jsch(privateKeyPath);
			setupSession(jsch);
			setupExecChannel();

			channelExec.setCommand(command);
			InputStream is = channelExec.getInputStream();

			channelExec.connect();

			return is;
		} catch (IOException | JSchException e) {
			throw new RuntimeException(e);
		}
	}

	private void setupExecChannel() {
		try {
			channelExec = (ChannelExec) jschSession.openChannel("exec");
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
	}

	public ChannelSftp sftp() {
		JSch jsch = jsch(privateKeyPath);
		setupSession(jsch);
		setupSftpChannel();

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

	private void setupSftpChannel() {
		try {
			channelSftp = (ChannelSftp) jschSession.openChannel("sftp");
			channelSftp.connect();
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
	}

	private void setupSession(JSch jsch) {
		setupSession(jsch, username, host, port);
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
		try {
			channelSftp.exit();
		} catch (NullPointerException ignored) {
		}
		try {
			channelExec.disconnect();
		} catch (NullPointerException ignored) {
		}
		jschSession.disconnect();
	}
}
