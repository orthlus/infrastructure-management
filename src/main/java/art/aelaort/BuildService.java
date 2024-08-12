package art.aelaort;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.stream.Collectors.joining;

@Component
public class BuildService {
	@Value("${build.data.config.path}")
	private String buildConfigPath;

	public void printConfig() {
		System.out.println(getConfigString());
	}

	@SneakyThrows
	public String getConfigString() {
		return Files.readAllLines(Path.of(buildConfigPath))
				.stream()
				.skip(2)
				.collect(joining("\n"));
	}
}
