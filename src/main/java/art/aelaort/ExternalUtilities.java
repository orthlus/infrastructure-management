package art.aelaort;

import art.aelaort.exceptions.DockerComposeValidationFailedException;
import art.aelaort.system.Response;
import art.aelaort.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class ExternalUtilities {
	private final SystemProcess systemProcess;
	@Value("${tabby.decode.script.bin}")
	private String tabbyDecodeScriptBin;
	@Value("${tabby.decode.script.file}")
	private String tabbyDecodeScriptFile;

	public void dockerComposeValidate(Path file) {
		String command = "docker compose -f %s config -q".formatted(file.toString());
		Response response = systemProcess.callProcess(command);

		if (response.exitCode() != 0) {
			throw new DockerComposeValidationFailedException(response);
		}
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
		String formatted = "%s %s %s %s".formatted(tabbyDecodeScriptBin, tabbyDecodeScriptFile, cipherFile, decodedFile);
		Response response = systemProcess.callProcess(formatted);

		if (response.exitCode() != 0) {
			throw new RuntimeException("tabbyDecode \n%s\n%s".formatted(response.stderr(), response.stdout()));
		}
	}
}
