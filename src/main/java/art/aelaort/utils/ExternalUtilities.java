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
	@Value("${tabby.decode.script.bin}")
	private String tabbyDecodeScriptBin;
	@Value("${tabby.decode.script.file}")
	private String tabbyDecodeScriptFile;
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

	public void ydSync() {
		System.out.println("yd sync started...");
		Response response = systemProcess.callProcess("wsl yandex-disk sync");

		if (response.exitCode() != 0) {
			throw new RuntimeException("ydSync error \n%s\n%s".formatted(response.stderr(), response.stdout()));
		}

		System.out.println("yd sync done");
	}

	public void tabbyDecode(Path cipherFile, Path decodedFile) {
		Response response = systemProcess.callProcess(
				tabbyDecodeScriptBin,
				tabbyDecodeScriptFile,
				cipherFile.toString(),
				decodedFile.toString());

		if (response.exitCode() != 0) {
			throw new RuntimeException("tabbyDecode \n%s\n%s".formatted(response.stderr(), response.stdout()));
		}
	}
}
