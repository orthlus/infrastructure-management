package art.aelaort.mappers;

import art.aelaort.models.servers.TabbyServer;
import art.aelaort.models.ssh.SshServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DockerMapper {
	@Value("${tabby.config.rsa_file_prefix}")
	private String tabbyConfigRsaFilePrefix;

	public SshServer map(TabbyServer tabbyServer) {
		return new SshServer(
				tabbyServer.host(),
				getKeyFullPath(tabbyServer),
				tabbyServer.port()
		);
	}

	private String getKeyFullPath(TabbyServer tabbyServer) {
		return tabbyConfigRsaFilePrefix
				.replace("file://", "")
				.replaceAll("\\\\", "/")
				+ tabbyServer.keyPath();
	}
}
