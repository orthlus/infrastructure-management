package art.aelaort.k8s;

import art.aelaort.models.servers.K8sCluster;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class K8sClusterProvider {
	private final K8sProps k8sProps;
	private final JsonMapper jsonMapper;

	public List<K8sCluster> getClustersFromLocalConfig() {
		return clustersParse(k8sProps.getSyncFile());
	}

	@SneakyThrows
	private List<K8sCluster> clustersParse(Path jsonPath) {
		return List.of(jsonMapper.readValue(jsonPath.toFile(), K8sCluster[].class));
	}
}
