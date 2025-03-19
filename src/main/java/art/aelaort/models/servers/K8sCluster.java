package art.aelaort.models.servers;

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
	@With
	private String name;
	private List<K8sApp> apps;
}
