package art.aelaort.models.servers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class K8sService {
	@JsonProperty
	private String name;
	@JsonProperty
	private String namespace;
	@JsonProperty
	private String kind;
	@JsonProperty
	private String type;
	@JsonProperty
	private String appSelector;
	@JsonProperty
	private Integer port;
	@JsonProperty
	private String targetPort;
	@JsonProperty
	private Integer nodePort;
	@JsonProperty
	private Boolean hasAnotherPorts;
}
