package art.aelaort.servers.providers;

import art.aelaort.models.servers.Server;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ServerProvider {
	private final JsonMapper jsonMapper;
	@Value("${servers.management.json_path}")
	private Path jsonDataPath;

	public List<Server> readLocalJsonData() {
		return serversParse(jsonDataPath);
	}

	@SneakyThrows
	private List<Server> serversParse(Path jsonPath) {
		return List.of(jsonMapper.readValue(jsonPath.toFile(), Server[].class));
	}
}
