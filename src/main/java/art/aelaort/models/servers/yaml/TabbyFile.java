package art.aelaort.models.servers.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class TabbyFile {
	List<Profile> profiles;

	@Getter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Profile {
		String name;
		String type;
		Option options;
	}

	@Getter
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Option {
		String host;
		Integer port;
		List<String> privateKeys;
	}
}
