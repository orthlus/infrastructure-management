package art.aelaort;

import art.aelaort.exceptions.BuildJobNotFoundException;
import art.aelaort.exceptions.TooManyDockerFilesException;
import art.aelaort.models.build.BuildType;
import art.aelaort.models.build.Job;
import art.aelaort.utils.ExternalUtilities;
import art.aelaort.utils.Utils;
import art.aelaort.utils.system.SystemProcess;
import com.fasterxml.jackson.databind.json.JsonMapper;
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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static art.aelaort.models.build.BuildType.java_docker;
import static art.aelaort.models.build.BuildType.java_graal_local;
import static art.aelaort.utils.Utils.log;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.chop;

@Component
@RequiredArgsConstructor
public class BuildService {
	private final ExternalUtilities externalUtilities;
	private final Utils utils;
	private final SystemProcess systemProcess;
	private final JsonMapper jsonMapper;
	private final DatabaseManageService databaseManageService;
	@Value("${build.data.config.path}")
	private Path buildConfigPath;
	@Value("${build.main.dir.secrets_dir}")
	private Path secretsRootDir;
	@Value("${build.main.src.dir}")
	private Path srcRootDir;
	@Value("${build.main.src.exclude.dirs}")
	private String[] excludeDirs;
	@Value("${build.main.default_files.dir}")
	private Path defaultFilesDir;
	@Value("${build.main.default_files.java_docker.path}")
	private String defaultJavaDockerfilePath;
	@Value("${build.main.docker.registry.url}")
	private String dockerRegistryUrl;
	@Value("${build.graalvm.artifact.name}")
	private String graalvmArtifactName;
	@Value("${build.main.bin.directory}")
	private Path binDirectory;

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
			runDbIfNeed(job);
			build(job, tmpDir, isBuildDockerNoCache);
			cleanTmp(tmpDir);
		}
	}

	private void runDbIfNeed(Job job) {
		if (job.isDb() && !databaseManageService.isLocalRunning()) {
			databaseManageService.localUp();
		}
	}

	private void cleanTmp(Path tmpDir) {
		FileUtils.deleteQuietly(tmpDir.toFile());
	}

	private void build(Job job, Path tmpDir, boolean isBuildDockerNoCache) {
		switch (job.getBuildType()) {
			case docker -> dockerBuildPush(job, tmpDir, isBuildDockerNoCache);
			case java_docker -> {
				run("mvn clean package", tmpDir);
				dockerBuildPush(job, tmpDir, isBuildDockerNoCache);
			}
			case java_local -> run("mvn clean install", tmpDir);
			case frontend_vue -> {
				run("yarn install", tmpDir);
				run("yarn run build", tmpDir);
				dockerBuildPush(job, tmpDir, isBuildDockerNoCache);
			}
			case java_graal_local -> {
				run("mvn clean native:compile -P native", tmpDir);
				copyArtifactToBinDirectory(java_graal_local, tmpDir);
			}
		}
	}

	private void copyArtifactToBinDirectory(BuildType type, Path tmpDir) {
		if (type == java_graal_local) {
			Path srcFile = tmpDir.resolve("target").resolve(graalvmArtifactName);
			Path destFile = binDirectory.resolve(graalvmArtifactName);

			try {
				FileUtils.copyFile(srcFile.toFile(), destFile.toFile(), false);
			} catch (Exception e) {
				log("error copy %s to %s, trying new name\n", srcFile, destFile);
				Path newDestFile = binDirectory.resolve("new-" + graalvmArtifactName);
				try {
					FileUtils.copyFile(srcFile.toFile(), newDestFile.toFile(), false);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		}
	}

	private void dockerBuildPush(Job job, Path tmpDir, boolean isBuildDockerNoCache) {
		String name = job.getName();
		Path dockerfile = lookupOneDockerfile(tmpDir);
		if (isBuildDockerNoCache) {
			run("docker build --no-cache -t %s:latest -f %s %s".formatted(name, dockerfile, tmpDir), tmpDir);
		} else {
			run("docker build -t %s:latest -f %s %s".formatted(name, dockerfile, tmpDir), tmpDir);
		}
		run("docker image tag %s:latest %s/%s:latest".formatted(name, dockerRegistryUrl, name), tmpDir);
		run("docker image push %s/%s:latest".formatted(dockerRegistryUrl, name), tmpDir);
	}

	private void run(String command, Path tmpDir) {
		systemProcess.callProcessForBuild(command, tmpDir);
	}

	private void copyDefaultDockerfile(Job job, Path tmpDir) {
		try {
			if (job.getBuildType() == java_docker) {
				if (notExistsAnyDockerfile(tmpDir)) {
					Path defaultFile = defaultFilesDir.resolve(defaultJavaDockerfilePath);
					Path dest = tmpDir.resolve(defaultFile.getFileName());
					FileUtils.copyFile(defaultFile.toFile(), dest.toFile(), false);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Path lookupOneDockerfile(Path dir) {
		return FileUtils.listFiles(dir.toFile(), dockerLookupFilter, null)
				.stream()
				.findFirst()
				.map(File::toPath)
				.orElseThrow(TooManyDockerFilesException::new);
	}

	private boolean notExistsAnyDockerfile(Path dir) {
		return FileUtils.listFiles(dir.toFile(), dockerLookupFilter, null).isEmpty();
	}

	private boolean isApproved(Job job) {
		return isApproved("do you want build app '%s' as '%s'? ".formatted(job.getName(), job.getBuildType()));
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
		return srcRootDir.resolve(job.getSubDirectory()).resolve(name);
	}

	public void fillSecretsToTmpDir(Job job, Path tmpDir) {
		fillSecretsToTmpDir(job, secretsRootDir, tmpDir);
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

	public Map<Integer, Job> getJobsMapById() {
		return readBuildConfig()
				.stream()
				.collect(Collectors.toMap(Job::getId, Function.identity()));
	}

	public Job getJobById(int id) {
		return readBuildConfig()
				.stream()
				.filter(job -> job.getId() == id)
				.findFirst()
				.orElseThrow(BuildJobNotFoundException::new);
	}

	@SneakyThrows
	private List<Job> readBuildConfig() {
		String jobsStr = externalUtilities.readBuildConfig();
		Job[] jobs = jsonMapper.readValue(jobsStr, Job[].class);
		return Arrays.asList(jobs);
	}

	public void printConfig() {
		log(getConfigString());
	}

	@SneakyThrows
	public String getConfigString() {
		return chop(chop(
				Files.readAllLines(buildConfigPath)
						.stream()
						.skip(3)
						.collect(joining("\n"))
		));
	}
}
