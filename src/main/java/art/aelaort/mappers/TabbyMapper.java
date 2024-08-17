package art.aelaort.mappers;

import art.aelaort.models.servers.TabbyServer;
import art.aelaort.models.servers.yaml.TabbyFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TabbyMapper {
	@Value("${tabby.config.rsa_file_prefix}")
	private String tabbyConfigRsaFilePrefix;

	public List<TabbyServer> map(TabbyFile tabbyFile) {
		return tabbyFile.getProfiles()
				.stream()
				.filter(profile -> profile.getType().equals("ssh"))
				.map(profile -> {
					String keyPath = profile.getOptions().getPrivateKeys().get(0);
					Integer filePort = profile.getOptions().getPort();
					return new TabbyServer(
							profile.getName(),
							profile.getOptions().getHost(),
							keyPath.replace(tabbyConfigRsaFilePrefix, ""),
							filePort != null ? filePort : 22
					);
				})
				.toList();
	}
}
