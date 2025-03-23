package art.aelaort.models.servers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder(toBuilder = true)
@Getter
@Accessors(fluent = true)
@Jacksonized
public class K8sCluster {
	@JsonProperty
	@With
	private String name;
	@JsonProperty
	private List<K8sApp> apps;
	@JsonProperty
	private List<String> nodes;
}
