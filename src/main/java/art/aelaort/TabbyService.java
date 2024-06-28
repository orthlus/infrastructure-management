package art.aelaort;

import art.aelaort.models.TabbyServer;
import art.aelaort.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TabbyService {
	private final SystemProcess systemProcess;
	private final TabbyS3 tabbyS3;
	@Value("${tabby.config.rsa_file_prefix}")
	private String tabbyConfigRsaFilePrefix;
	@Value("${tabby.config.path}")
	private String tabbyConfigPath;
	@Value("${tabby.decode.script.bin}")
	private String decodeScriptBin;
	@Value("${tabby.decode.script.file}")
	private String decodeScriptFile;
	@Value("${tmp.dir}")
	private String tmpDir;

	private final Yaml yaml;

	public List<TabbyServer> parseLocalFile() {
		try {
			String content = Files.readString(Path.of(tabbyConfigPath));
			return parse(content);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String getRemoteFileContent() {
		String downloaded = tabbyS3.download().split("\n")[1];
		return decode(downloaded);
	}

	@SuppressWarnings("unchecked")
	private List<TabbyServer> parse(String content) {
		Map<String, Object> obj = yaml.load(content);
		List<Map<String, Object>> profiles = (ArrayList<Map<String, Object>>) obj.get("profiles");

		List<TabbyServer> result = new ArrayList<>(profiles.size());

		for (Map<String, Object> profile : profiles) {
			String type = (String) profile.get("type");
			if (!type.equals("ssh")) {
				continue;
			}

			String name = (String) profile.get("name");
			String host = (String) ((Map<String, Object>) profile.get("options")).get("host");
			String keyPath = ((List<String>) ((Map<String, Object>) profile.get("options")).get("privateKeys")).get(0)
					.replace(tabbyConfigRsaFilePrefix, "");
			result.add(new TabbyServer(name, host, keyPath));
		}

		return result;
	}

	private String decode(String encrypted) {
		try {
			Path cipherFile = Path.of(tmpDir + UUID.randomUUID());
			Path decodedFile = Path.of(tmpDir + UUID.randomUUID());

			Files.writeString(cipherFile, encrypted);
			systemProcess.callProcess("%s %s %s %s".formatted(decodeScriptBin, decodeScriptFile, cipherFile, decodedFile));
			String result = Files.readString(decodedFile);

			Files.deleteIfExists(cipherFile);
			Files.deleteIfExists(decodedFile);

			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
