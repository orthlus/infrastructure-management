package art.aelaort.ssh.tw;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SshKeyPost(
		String body,
		String name,
		@JsonProperty("is_default")
		boolean isDefault
) {
}
