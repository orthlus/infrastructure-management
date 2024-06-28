package art.aelaort;

import art.aelaort.models.DirServer;
import art.aelaort.models.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ServersManagementService {
	private final Yaml yaml;
	@Value("${servers.management.dir}")
	private String serversDir;
	@Value("${servers.management.files.monitoring}")
	private String monitoringFile;
	@Value("${servers.management.files.not_scan}")
	private String notScanFile;
	@Value("${servers.management.custom_projects_file}")
	private String projectsYmlFileName;

	public ServersManagementService(Yaml yaml) {
		this.yaml = yaml;
	}

	public List<DirServer> getDirServers() {
		List<Path> serversDirs = getServersDirs();
		List<DirServer> result = new ArrayList<>(serversDirs.size() * 2);

		for (Path serverDir : serversDirs) {
			for (Path ymlFile : findYmlFiles(serverDir)) {
				String file = ymlFile.getFileName().toString();
				if (file.contains("docker")) {
					result.add(parseDockerYmlFile(ymlFile));
				} else if (file.equals(projectsYmlFileName)) {
					result.add(parseCustomYmlFile(ymlFile));
				}
			}
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private DirServer parseCustomYmlFile(Path ymlFile) {
		Path serverDir = ymlFile.getParent();
		boolean monitoring = serverDir.resolve(monitoringFile).toFile().exists();
		String content = readFile(ymlFile);

		Map<String, Object> load = yaml.load(content);
		List<String> projects = (List<String>) load.get("projects");
		List<Service> services = new ArrayList<>();
		for (String project : projects) {
			services.add(new Service(project, ymlFile.getFileName().toString()));
		}
		return new DirServer(serverDir.getFileName().toString(), monitoring, services);
	}

	@SuppressWarnings("unchecked")
	private DirServer parseDockerYmlFile(Path ymlFile) {
		Path serverDir = ymlFile.getParent();
		boolean monitoring = serverDir.resolve(monitoringFile).toFile().exists();
		String content = readFile(ymlFile);

		Map<String, Object> load = yaml.load(content);
		Map<String, Object> services = (Map<String, Object>) load.get("services");
		List<Service> resultServices = new ArrayList<>();
		for (Map.Entry<String, Object> service : services.entrySet()) {
			String serviceName = service.getKey();
			Map<String, Object> params = (Map<String, Object>) service.getValue();
			if (params.containsKey("container_name")) {
				String containerName = (String) params.get("container_name");
				resultServices.add(new Service(containerName, ymlFile.getFileName().toString(), serviceName));
			} else {
				resultServices.add(new Service(serviceName, ymlFile.getFileName().toString()));
			}
		}
		return new DirServer(serverDir.getFileName().toString(), monitoring, resultServices);
	}

	private String readFile(Path ymlFile) {
		try {
			return Files.readString(ymlFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<Path> findYmlFiles(Path dir) {
		try {
			return Files.walk(dir, 1)
					.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".yml"))
					.toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Path> getServersDirs() {
		try {
			Path dir = Path.of(serversDir);
			return Files.walk(dir, 1)
					.filter(path -> !path.equals(dir))
					.filter(path -> path.toFile().isDirectory())
					.filter(path -> !path.resolve(notScanFile).toFile().exists())
					.toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
