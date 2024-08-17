package art.aelaort;

import art.aelaort.mappers.TabbyMapper;
import art.aelaort.models.servers.TabbyServer;
import art.aelaort.models.servers.yaml.TabbyFile;
import art.aelaort.s3.TabbyS3;
import art.aelaort.utils.ExternalUtilities;
import art.aelaort.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TabbyService {
	private final TabbyS3 tabbyS3;
	private final ExternalUtilities externalUtilities;
	private final Utils utils;
	private final ObjectMapper yamlMapper;
	private final TabbyMapper tabbyMapper;
	@Value("${tabby.config.path}")
	private Path tabbyConfigPath;

	public List<TabbyServer> getServersFromLocalFile() {
		try {
			TabbyFile tabbyFile = yamlMapper.readValue(tabbyConfigPath.toFile(), TabbyFile.class);
			return tabbyMapper.map(tabbyFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void downloadFileToLocal(boolean logging) {
		try {
			String remoteFileContent = getRemoteFileContent();
			Files.writeString(tabbyConfigPath, remoteFileContent);
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

	private String decode(String encrypted) {
		try {
			Path tmpDir = utils.createTmpDir();
			Path cipherFile = tmpDir.resolve(UUID.randomUUID().toString());
			Path decodedFile = tmpDir.resolve(UUID.randomUUID().toString());

			Files.writeString(cipherFile, encrypted);
			externalUtilities.tabbyDecode(cipherFile, decodedFile);
			String result = Files.readString(decodedFile);

			FileUtils.deleteQuietly(tmpDir.toFile());

			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
