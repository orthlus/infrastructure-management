package art.aelaort.models.servers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class K8sApp {
	@JsonProperty
	private String name;
	@JsonProperty
	private String namespace;
	@JsonProperty
	private String podName;
	@JsonProperty
	@With
	private String image;
	@JsonProperty
	private String kind;
	@JsonProperty
	private String schedule;
	@JsonProperty
	private String strategyType;
	@JsonProperty
	private String containerName;
	@JsonProperty
	private String imagePullPolicy;
}
