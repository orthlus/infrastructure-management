package art.aelaort;

import art.aelaort.exceptions.BuildJobNotFoundException;
import art.aelaort.models.build.Job;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.stream.Stream;

import static art.aelaort.models.build.BuildType.*;
import static java.nio.file.Path.of;
import static java.util.stream.Collectors.joining;

@Component
@RequiredArgsConstructor
public class BuildService {
	private final ExternalUtilities externalUtilities;
	private final Utils utils;
	@Value("${build.data.config.path}")
	private String buildConfigPath;
	@Value("${build.main.dir.secrets_dir}")
	private String secretsRootDir;
	@Value("${build.main.src.dir}")
	private String srcRootDir;
	@Value("${build.main.src.exclude.dirs}")
	private String[] excludeDirs;
	@Value("${build.main.default_files.dir}")
	private String defaultFilesDir;
	@Value("${build.main.default_files.java_docker.path}")
	private String defaultJavaDockerfilePath;

	private FileFilter excludeDirsFilter;
	private IOFileFilter dockerLookupFilter;

	@PostConstruct
	private void init() {
		excludeDirsFilter = new OrFileFilter(Stream
				.of(excludeDirs)
				.map(FileFilterUtils::nameFileFilter)
				.map(FileFilterUtils::makeDirectoryOnly)
				.toList()
		).negate();
		dockerLookupFilter = FileFilterUtils.suffixFileFilter("dockerfile", IOCase.INSENSITIVE);
	}

	public void run(Job job, boolean isBuildDockerNoCache) {
		if (isApproved(job)) {
			Path tmpDir = utils.createTmpDir();
			copySrcDirToTmpDir(job, tmpDir);
			copyDefaultDockerfile(job, tmpDir);
			fillSecretsToTmpDir(job, tmpDir);
			build(job, tmpDir, isBuildDockerNoCache);
			cleanTmp(tmpDir);
		}
	}

	private void cleanTmp(Path tmpDir) {
		FileUtils.deleteQuietly(tmpDir.toFile());
	}

	private void build(Job job, Path tmpDir, boolean isBuildDockerNoCache) {

	}

	private void copyDefaultDockerfile(Job job, Path tmpDir) {
		try {
			if (job.getBuildType() == java_docker) {
				if (notExistsAnyDockerfile(tmpDir)) {
					Path defaultFile = of(defaultFilesDir).resolve(defaultJavaDockerfilePath);
					Path dest = tmpDir.resolve(defaultFile.getFileName());
					FileUtils.copyFile(defaultFile.toFile(), dest.toFile(), false);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean notExistsAnyDockerfile(Path dir) {
		return FileUtils.listFiles(dir.toFile(), dockerLookupFilter, null).isEmpty();
	}

	private boolean isApproved(Job job) {
		return isApproved("do you want build app '%s'? ".formatted(job.getName()));
	}

	private boolean isApproved(String text) {
		Scanner scanner = new Scanner(System.in);
		System.out.print(text);
		String answer = scanner.nextLine()
				.replace("\n", "")
				.replace("\r", "");

		return answer.equals("y") || answer.equals("d");
	}

	@SneakyThrows
	public void copySrcDirToTmpDir(Job job, Path tmpDir) {
		FileUtils.copyDirectory(
				getSrcDir(job).toFile(),
				tmpDir.toFile(),
				excludeDirsFilter,
				false);
	}

	public Path getSrcDir(Job job) {
		String name = job.getProjectDir() == null ? job.getName() : job.getProjectDir();
		return of(srcRootDir).resolve(job.getSubDirectory()).resolve(name);
	}

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
