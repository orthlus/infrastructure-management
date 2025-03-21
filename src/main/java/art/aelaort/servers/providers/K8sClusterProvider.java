package art.aelaort.servers.providers;

import art.aelaort.data.parsers.K8sYamlParser;
import art.aelaort.k8s.K8sProps;
import art.aelaort.models.servers.K8sCluster;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class K8sClusterProvider {
	private final K8sYamlParser k8sYamlParser;
	private final K8sProps k8sProps;
	private final JsonMapper jsonMapper;
	@Value("${servers.management.files.not_scan}")
	private String notScanFile;

	public List<K8sCluster> getClustersFromLocalConfig() {
		return clustersParse(k8sProps.getSyncFile());
	}

	@SneakyThrows
	private List<K8sCluster> clustersParse(Path jsonPath) {
		return List.of(jsonMapper.readValue(jsonPath.toFile(), K8sCluster[].class));
	}

	public List<K8sCluster> getClusters() {
		List<K8sCluster> result = new ArrayList<>();

		for (Path clustersDir : getClustersDirs()) {
			Path dir = clustersDir.resolve(k8sProps.getPathFiles());

			K8sCluster k8sCluster = K8sCluster.builder()
					.name(clustersDir.getFileName().toString())
					.apps(getYamlFiles(dir)
							.stream()
							.map(k8sYamlParser::parseK8sYmlFile)
							.flatMap(Collection::stream)
							.toList())
					.build();

			result.add(k8sCluster);
		}
		return result;
	}

	private List<Path> getYamlFiles(Path dir) {
		try (Stream<Path> walk = Files.walk(dir)) {
			return walk
					.filter(path -> {
						String lowerCase = path.getFileName().toString().toLowerCase();
						return lowerCase.endsWith(".yml") || lowerCase.endsWith(".yaml");
					})
					.toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<Path> getClustersDirs() {
		try (Stream<Path> walk = Files.walk(k8sProps.getDir(), 1)) {
			return walk
					.filter(path -> !path.equals(k8sProps.getDir()))
					.filter(path -> path.toFile().isDirectory())
					.filter(path -> !path.resolve(notScanFile).toFile().exists())
					.filter(path -> path.resolve(k8sProps.getPathFiles()).toFile().exists())
					.toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
