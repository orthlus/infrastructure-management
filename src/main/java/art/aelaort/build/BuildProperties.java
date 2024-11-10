package art.aelaort.build;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Getter
@Component
@Accessors(fluent = true)
public class BuildProperties {
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
	@Value("${build.main.default.java.version}")
	private int defaultJavaVersion;
	@Value("${build.main.docker.registry.url}")
	private String dockerRegistryUrl;
	@Value("${build.main.docker.remote.name}")
	private String dockerRemoteName;
	@Value("${build.main.docker.remote.tmp.root}")
	private String dockerRemoteTmpRootDir;
	@Value("${build.graalvm.artifact.name}")
	private String graalvmArtifactName;
	@Value("${build.main.bin.directory}")
	private Path binDirectory;
	@Value("${tmp.root.dir}")
	private Path localTmpRootDir;
}
