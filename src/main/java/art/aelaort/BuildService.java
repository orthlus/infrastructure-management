package art.aelaort;

import art.aelaort.exceptions.BuildJobNotFoundException;
import art.aelaort.models.build.Job;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Path.of;
import static java.util.stream.Collectors.joining;

@Component
@RequiredArgsConstructor
public class BuildService {
	private final ExternalUtilities externalUtilities;
	@Value("${build.data.config.path}")
	private String buildConfigPath;
	@Value("${build.main.dir.secrets_dir}")
	private String secretsRootDir;

	public void fillSecretsToTmpDir(Job job, Path tmpDir) {
		fillSecretsToTmpDir(job, of(secretsRootDir), tmpDir);
	}

	@SneakyThrows
	private void fillSecretsToTmpDir(Job job, Path secretsRoot, Path tmpDir) {
		if (job.getSecretsDirectory() != null) {
			Path src = secretsRoot.resolve(job.getSecretsDirectory());
			FileUtils.copyDirectory(src.toFile(), tmpDir.toFile(), false);
		}
	}

	public boolean isBuildDockerNoCache(String[] args) {
		for (String arg : args) {
			if (arg.equals("clean")) {
				return true;
			}
		}
		return false;
	}

	public Job getJobById(int id) {
		return externalUtilities.readBuildConfig()
				.stream()
				.filter(job -> job.getId() == id)
				.findFirst()
				.orElseThrow(BuildJobNotFoundException::new);
	}

	public void printConfig() {
		System.out.println(getConfigString());
	}

	@SneakyThrows
	public String getConfigString() {
		return Files.readAllLines(of(buildConfigPath))
				.stream()
				.skip(2)
				.collect(joining("\n"));
	}
}
