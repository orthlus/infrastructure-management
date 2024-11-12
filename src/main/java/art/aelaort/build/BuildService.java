package art.aelaort.build;

import art.aelaort.DatabaseManageService;
import art.aelaort.exceptions.TooManyDockerFilesException;
import art.aelaort.models.build.BuildType;
import art.aelaort.models.build.Job;
import art.aelaort.models.build.PomModel;
import art.aelaort.properties.S3Properties;
import art.aelaort.s3.BuildFunctionsS3;
import art.aelaort.utils.Utils;
import art.aelaort.utils.system.SystemProcess;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.springframework.stereotype.Component;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Stream;

import static art.aelaort.models.build.BuildType.java_docker;
import static art.aelaort.models.build.BuildType.java_graal_local;
import static art.aelaort.utils.Utils.log;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.chop;

@Slf4j
@Component
@RequiredArgsConstructor
public class BuildService {
	private final Utils utils;
	private final SystemProcess systemProcess;
	private final DatabaseManageService databaseManageService;
	private final XmlMapper xmlMapper;
	private final BuildFunctionsS3 buildFunctionsS3;
	private final S3Properties s3Properties;
	private final BuildProperties buildProperties;
	private final JobsTextTable jobsTextTable;
	private final JobsProvider jobsProvider;

	private final IOFileFilter dockerLookupFilter =
			FileFilterUtils.suffixFileFilter("dockerfile", IOCase.INSENSITIVE);

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
				run("mvn clean package -DskipTests", tmpDir);
				dockerBuildPush(job, tmpDir, isBuildDockerNoCache);
			}
			case java_local -> run("mvn clean install -DskipTests", tmpDir);
			case frontend_vue -> {
				run("yarn install", tmpDir);
				run("yarn run build", tmpDir);
				dockerBuildPush(job, tmpDir, isBuildDockerNoCache);
			}
			case java_graal_local -> {
				run("mvn clean native:compile -P native", tmpDir);
				copyArtifactToBinDirectory(java_graal_local, tmpDir);
			}
			case ya_func -> srcZipToS3(job, tmpDir);
		}
	}

	private void srcZipToS3(Job job, Path tmpDir) {
		Path zipFile = zipDir(job, tmpDir);
		buildFunctionsS3.uploadZip(zipFile);
		log("for job '%s' to s3 bucket '%s' uploaded zip file '%s'\n",
				job.getName(),
				s3Properties.getBuild().getBucket(),
				zipFile.getFileName());
		cleanTmp(zipFile.getParent());
	}

	private Path zipDir(Job job, Path tmpDir) {
		Path zipFile = utils.createTmpDir().resolve(job.getName() + ".zip");
		ZipUtil.pack(tmpDir.toFile(), zipFile.toFile(), 0);
		return zipFile;
	}

	private void copyArtifactToBinDirectory(BuildType type, Path tmpDir) {
		String graalvmArtifactName = buildProperties.graalvmArtifactName();
		Path binDirectory = buildProperties.binDirectory();

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
			run("docker build --no-cache -t %s:latest -f %s %s".formatted(name, dockerfile, tmpDir), null);
		} else {
			run("docker build -t %s:latest -f %s %s".formatted(name, dockerfile, tmpDir), null);
		}
		run("docker image tag %s:latest %s/%s:latest".formatted(name, buildProperties.dockerRegistryUrl(), name), null);
		run("docker image push %s/%s:latest".formatted(buildProperties.dockerRegistryUrl(), name), null);
	}

	private void run(String command, Path tmpDir) {
		systemProcess.callProcessForBuild(command, tmpDir);
	}

	private void copyDefaultDockerfile(Job job, Path tmpDir) {
		try {
			if (job.getBuildType() == java_docker) {
				if (notExistsAnyDockerfile(tmpDir)) {
					Path defaultFile = buildProperties.defaultFilesDir().resolve(getDefaultJavaDockerfilePath(tmpDir));
					Path dest = tmpDir.resolve(defaultFile.getFileName());
					FileUtils.copyFile(defaultFile.toFile(), dest.toFile(), false);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String getDefaultJavaDockerfilePath(Path tmpDir) {
		PomModel pomModel = parsePomFile(tmpDir.resolve("pom.xml"));
		Integer mavenCompilerTarget = pomModel.getMavenCompilerTarget();
		return buildProperties.defaultJavaDockerfilePath()
				.formatted(mavenCompilerTarget == null ? buildProperties.defaultJavaVersion() : mavenCompilerTarget);
	}

	@SneakyThrows
	private PomModel parsePomFile(Path file) {
		return xmlMapper.readValue(file.toFile(), PomModel.class);
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
		FileFilter excludeDirsFilter = new OrFileFilter(Stream
				.of(buildProperties.excludeDirs())
				.map(FileFilterUtils::nameFileFilter)
				.map(FileFilterUtils::makeDirectoryOnly)
				.toList()
		).negate();
		FileUtils.copyDirectory(
				getSrcDir(job).toFile(),
				tmpDir.toFile(),
				excludeDirsFilter,
				false);
	}

	public Path getSrcDir(Job job) {
		String name = job.getProjectDir() == null ? job.getName() : job.getProjectDir();
		return buildProperties.srcRootDir().resolve(job.getSubDirectory()).resolve(name);
	}

	public void fillSecretsToTmpDir(Job job, Path tmpDir) {
		fillSecretsToTmpDir(job, buildProperties.secretsRootDir(), tmpDir);
	}

	@SneakyThrows
	private void fillSecretsToTmpDir(Job job, Path secretsRoot, Path tmpDir) {
		if (job.getSecretsDirectory() != null) {
			Path src = secretsRoot.resolve(job.getSecretsDirectory());
			FileUtils.copyDirectory(src.toFile(), tmpDir.toFile(), false);
		}
	}

	public boolean isBuildDockerNoCache(String[] args) {
		return Arrays.asList(args).contains("clean");
	}

	public void printConfig() {
		log(jobsTextTable.getJobsTableString(jobsProvider.readBuildConfig()));
	}

	@SneakyThrows
	public String getConfigString() {
		return chop(chop(
				Files.readAllLines(buildProperties.buildConfigPath())
						.stream()
						.skip(3)
						.collect(joining("\n"))
		));
	}
}
