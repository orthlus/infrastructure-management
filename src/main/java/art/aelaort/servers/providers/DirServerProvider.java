package art.aelaort.servers.providers;

import art.aelaort.data.parsers.CustomProjectYamlParser;
import art.aelaort.data.parsers.DockerComposeParser;
import art.aelaort.models.servers.DirServer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class DirServerProvider {
	private final DockerComposeParser dockerComposeParser;
	private final CustomProjectYamlParser customProjectYamlParser;
	@Value("${servers.management.custom_projects_file}")
	private String projectsYmlFileName;
	@Value("${servers.management.dir}")
	private Path serversDir;
	@Value("${servers.management.files.not_scan}")
	private String notScanFile;

	public List<DirServer> scanServersDir() {
		List<Path> serversDirs = scanLocalDirs();
		List<DirServer> result = new ArrayList<>(serversDirs.size() * 2);

		for (Path serverDir : serversDirs) {
			for (Path ymlFile : findYmlFiles(serverDir)) {
				String file = ymlFile.getFileName().toString();
				if (file.contains("docker")) {
					result.add(dockerComposeParser.parseDockerYmlFile(ymlFile));
				} else if (file.equals(projectsYmlFileName)) {
					result.add(customProjectYamlParser.parseCustomYmlFile(ymlFile));
				}
			}
		}

		return result;
	}

	private List<Path> findYmlFiles(Path dir) {
		try (Stream<Path> walk = Files.walk(dir, 1)) {
			return walk
					.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".yml"))
					.toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<Path> scanLocalDirs() {
		try (Stream<Path> walk = Files.walk(serversDir, 1)) {
			return walk
					.filter(path -> !path.equals(serversDir))
					.filter(path -> path.toFile().isDirectory())
					.filter(path -> !path.resolve(notScanFile).toFile().exists())
					.toList();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
