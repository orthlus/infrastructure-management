package art.aelaort.k8s;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@ConfigurationProperties("k8s")
@Getter
@Setter
public class K8sProps {
	private Path dir;
	private Path clustersListFile;
	private String pathFiles;
	private Path syncFile;
	private String nodesFile;
	private String configPath;
}
