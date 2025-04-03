package art.aelaort.utils;

import art.aelaort.utils.system.Response;
import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExternalUtilities {
	private final SystemProcess systemProcess;
	@Value("${build.data.config.bin}")
	private String buildConfigBin;
	@Value("${build.data.config.converter.path}")
	private String buildConfigConverterPath;

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
}
