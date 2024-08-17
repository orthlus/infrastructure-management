package art.aelaort.mappers;

import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.TabbyServer;
import art.aelaort.models.ssh.SshServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DockerMapper {
	@Value("${tabby.config.rsa_file_prefix}")
	private String tabbyConfigRsaFilePrefix;

	public SshServer map(Server server) {
		return new SshServer(
				server.getIp(),
				getKeyFullPath(server.getSshKey()),
				server.getPort(),
				server.getName()
		);
	}

	public SshServer map(TabbyServer tabbyServer) {
		return new SshServer(
				tabbyServer.host(),
				getKeyFullPath(tabbyServer.keyPath()),
				tabbyServer.port(),
				tabbyServer.name()
		);
	}

	private String getKeyFullPath(String relativeKeyPath) {
		return tabbyConfigRsaFilePrefix
				.replace("file://", "")
				.replaceAll("\\\\", "/")
				+ relativeKeyPath;
	}
}
