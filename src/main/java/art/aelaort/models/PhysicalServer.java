package art.aelaort.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

import static java.lang.String.join;

@Getter
@NoArgsConstructor
public class PhysicalServer {
	@JsonProperty
	private String name;
	@JsonProperty
	private String ip;
	@JsonProperty
	private String sshKey;
	@JsonProperty
	private boolean monitoring;
	@JsonProperty
	private List<Service> services;
	@JsonProperty
	private String servicesStr;

	public PhysicalServer(String name, String ip, String sshKey, boolean monitoring, List<Service> services) {
		this.name = name;
		this.ip = ip;
		this.sshKey = sshKey;
		this.monitoring = monitoring;
		this.services = services;
		this.servicesStr = servicesStr(services);
	}

	private static String servicesStr(List<Service> services) {
		return join(", ", services.stream()
				.map(s -> s.getDockerName() != null ? s.getDockerName() : s.getService())
				.toList());
	}
}
