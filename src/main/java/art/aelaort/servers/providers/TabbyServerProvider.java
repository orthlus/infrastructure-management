package art.aelaort.servers.providers;

import art.aelaort.mappers.TabbyMapper;
import art.aelaort.models.servers.TabbyServer;
import art.aelaort.models.servers.yaml.TabbyFile;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TabbyServerProvider {
	private final YAMLMapper yamlMapper;
	private final TabbyMapper tabbyMapper;
	@Value("${tabby.config.path}")
	private Path tabbyConfigPath;

	public List<TabbyServer> readLocal() {
		TabbyFile tabbyFile = parseFile(tabbyConfigPath);
		return tabbyMapper.map(tabbyFile);
	}

	@SneakyThrows
	private TabbyFile parseFile(Path tabbyConfigPath) {
		return yamlMapper.readValue(tabbyConfigPath.toFile(), TabbyFile.class);
	}
}
