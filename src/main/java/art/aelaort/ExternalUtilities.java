package art.aelaort;

import art.aelaort.exceptions.DockerComposeValidationFailedException;
import art.aelaort.exceptions.ReadingBuildConfigException;
import art.aelaort.models.build.Job;
import art.aelaort.system.Response;
import art.aelaort.system.SystemProcess;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExternalUtilities {
	private final SystemProcess systemProcess;
	private final ObjectMapper jackson;
	@Value("${tabby.decode.script.bin}")
	private String tabbyDecodeScriptBin;
	@Value("${tabby.decode.script.file}")
	private String tabbyDecodeScriptFile;
	@Value("${build.data.config.bin}")
	private String buildConfigBin;
	@Value("${build.data.config.converter.path}")
	private String buildConfigConverterPath;

	public List<Job> readBuildConfig() {
		String command = buildConfigBin + " " + buildConfigConverterPath;
		Response response = systemProcess.callProcess(command);

		try {
			if (response.exitCode() == 0) {
				String jsonStr = response.stdout();
				Job[] jobs = jackson.readValue(jsonStr, Job[].class);
				return Arrays.asList(jobs);
			} else {
				throw new ReadingBuildConfigException();
			}
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

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
