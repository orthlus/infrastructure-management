package art.aelaort.models.servers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

import static java.lang.String.join;

@Getter
@NoArgsConstructor
public class Server {
	@JsonProperty
	private String name;
	@JsonProperty
	private String ip;
	@JsonProperty
	private String sshKey;
	@JsonProperty
	private int port;
	@JsonProperty
	private boolean monitoring;
	@JsonProperty
	private List<ServiceDto> services;
	@JsonProperty
	private String servicesStr;

	public Server(String name, String ip, String sshKey, int port, boolean monitoring, List<ServiceDto> services) {
		this.name = name;
		this.ip = ip;
		this.sshKey = sshKey;
		this.port = port;
		this.monitoring = monitoring;
		this.services = services;
		this.servicesStr = servicesStr(services);
	}

	private static String servicesStr(List<ServiceDto> services) {
		return join(", ", services.stream()
				.map(s -> s.getDockerName() != null ? s.getDockerName() : s.getService())
				.toList());
	}
}
