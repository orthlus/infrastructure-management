package art.aelaort;

import art.aelaort.exceptions.TabbyServerByPortTooManyServersException;
import art.aelaort.exceptions.TabbyServerNotFoundException;
import art.aelaort.models.TabbyServer;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class TabbyService {
	private final TabbyS3 tabbyS3;
	private final ExternalUtilities externalUtilities;
	@Value("${tabby.config.rsa_file_prefix}")
	private String tabbyConfigRsaFilePrefix;
	@Value("${tabby.config.path}")
	private String tabbyConfigPath;
	@Value("${tmp.dir}")
	private String tmpDir;

	private final Yaml yaml;

	public String getKeyFullPath(TabbyServer tabbyServer) {
		return tabbyConfigRsaFilePrefix
				.replace("file://", "")
				.replaceAll("\\\\", "/")
				+ tabbyServer.keyPath();
	}

	public TabbyServer findTabbyServer(String nameOrPort) {
		try {
			return getServerByPortNumber(Integer.parseInt(nameOrPort));
		} catch (NumberFormatException e) {
			return getServerByName(nameOrPort);
		}
	}

	public TabbyServer getServerByPortNumber(int port) {
		List<TabbyServer> list = parseLocalFile().stream()
				.filter(s -> s.port() == port)
				.toList();
		return switch (list.size()) {
			case 0 -> throw new TabbyServerNotFoundException();
			case 1 -> list.get(0);
			default -> throw new TabbyServerByPortTooManyServersException();
		};
	}

	public TabbyServer getServerByName(String name) {
		return parseLocalFile().stream()
				.filter(s -> s.name().equals(name))
				.findFirst()
				.orElseThrow(TabbyServerNotFoundException::new);
	}

	public List<TabbyServer> parseLocalFile() {
		try {
			String content = Files.readString(Path.of(tabbyConfigPath));
			return parse(content);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void downloadFileToLocal(boolean logging) {
		try {
			String remoteFileContent = getRemoteFileContent();
			Files.writeString(Path.of(tabbyConfigPath), remoteFileContent);
			if (logging) {
				System.out.println("tabby config downloaded");
			}
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
			Map<String, Object> options = (Map<String, Object>) profile.get("options");
			String host = (String) options.get("host");
			List<String> keys = (List<String>) options.get("privateKeys");
			String keyPath = keys.get(0).replace(tabbyConfigRsaFilePrefix, "");
			int port;
			if (options.containsKey("port")) {
				port = (int) options.get("port");
			} else {
				port = 22;
			}
			result.add(new TabbyServer(name, host, keyPath, port));
		}

		return result;
	}

	private String decode(String encrypted) {
		try {
			Path cipherFile = Path.of(tmpDir + UUID.randomUUID());
			Path decodedFile = Path.of(tmpDir + UUID.randomUUID());

			Files.writeString(cipherFile, encrypted);
			externalUtilities.tabbyDecode(cipherFile, decodedFile);
			String result = Files.readString(decodedFile);

			Files.deleteIfExists(cipherFile);
			Files.deleteIfExists(decodedFile);

			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
