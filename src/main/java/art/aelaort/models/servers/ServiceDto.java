package art.aelaort.models.servers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDto {
	private String service;
	private String ymlName;
	private String dockerName;
	@Setter
	private String dockerImageName;

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
