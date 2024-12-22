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
import java.util.List;
import java.util.Optional;
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
		return scanLocalDirs()
				.stream()
				.map(this::findYmlFile)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.map(this::parseYmlFile)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
	}

	private Optional<DirServer> parseYmlFile(Path ymlFile) {
		String file = ymlFile.getFileName().toString();
		if (file.equals(projectsYmlFileName)) {
			return Optional.of(customProjectYamlParser.parseCustomYmlFile(ymlFile));
		} else if (file.contains("docker")) {
			return Optional.of(dockerComposeParser.parseDockerYmlFile(ymlFile));
		}
		return Optional.empty();
	}

	private Optional<Path> findYmlFile(Path dir) {
		try (Stream<Path> walk = Files.walk(dir, 1)) {
			List<Path> paths = walk
					.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".yml"))
					.toList();
			if (paths.size() == 1) {
				return Optional.of(paths.get(0));
			}
			if (paths.size() > 1) {
				Optional<Path> pathNonDockerOp = paths.stream()
						.filter(path -> path.getFileName().toString().equals(projectsYmlFileName))
						.findFirst();
				if (pathNonDockerOp.isPresent()) {
					return pathNonDockerOp;
				} else {
					return paths.stream()
							.filter(path -> path.getFileName().toString().contains("docker"))
							.findFirst();
				}
			}

			return Optional.empty();
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
