package art.aelaort.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

import static java.lang.String.join;

@Getter
@AllArgsConstructor
public class PhysicalServer {
	private String name;
	private String ip;
	private String sshKey;
	private boolean monitoring;
	private List<Service> services;
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
