package art.aelaort.models;

import lombok.Getter;

@Getter
public class Service {
	private final String service;
	private final String ymlName;
	private String dockerName;

	public Service(String service, String ymlName, String dockerName) {
		this.service = service;
		this.ymlName = ymlName;
		this.dockerName = dockerName;
	}

	public Service(String service, String ymlName) {
		this.service = service;
		this.ymlName = ymlName;
	}

	@Override
	public String toString() {
		if (dockerName == null) {
			return "%s %s".formatted(service, ymlName);
		} else {
			return "%s (%s) %s".formatted(dockerName, service, ymlName);
		}
	}
}
