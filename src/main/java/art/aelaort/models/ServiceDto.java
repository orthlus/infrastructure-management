package art.aelaort.models;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ServiceDto {
	private String service;
	private String ymlName;
	private String dockerName;

	public ServiceDto(String service, String ymlName, String dockerName) {
		this.service = service;
		this.ymlName = ymlName;
		this.dockerName = dockerName;
	}

	public ServiceDto(String service, String ymlName) {
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
