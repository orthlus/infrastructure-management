package art.aelaort.utils;

import art.aelaort.utils.system.Response;
import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ExternalUtilities {
	private final SystemProcess systemProcess;
	@Value("${build.data.config.bin}")
	private String buildConfigBin;
	@Value("${build.data.config.converter.path}")
	private String buildConfigConverterPath;

	public void dockerPs() {
		systemProcess.callProcessInheritIO("docker ps -a");
	}

	public String readBuildConfig() {
		Response response = systemProcess.callProcess(buildConfigBin, buildConfigConverterPath);
		if (response.exitCode() == 0) {
			return response.stdout();
		}
		throw new RuntimeException(response.stderr());
	}

	public Optional<String> dockerComposeValidate(Path file) {
		String command = "docker compose -f %s config -q".formatted(file.toString());
		Response response = systemProcess.callProcess(command);

		return response.exitCode() == 0 ? Optional.empty() : Optional.of(response.stderr());
	}

	public void dirSync() {
		Response response = systemProcess.callProcessInheritFilteredStdout(
				stdout -> !stdout.contains("/.git/") && !stdout.contains("Completed "), "workdir-sync.bat");

		if (response.exitCode() != 0) {
			throw new RuntimeException("dir sync error \n%s\n%s".formatted(response.stderr(), response.stdout()));
		}

		int gitRows = StringUtils.countMatches(response.stdout(), "/.git/");
		if (gitRows > 0) {
			log("also synced .git: %d paths\n", gitRows);
		}
	}
}
