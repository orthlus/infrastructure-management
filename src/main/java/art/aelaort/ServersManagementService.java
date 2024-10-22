package art.aelaort;

import art.aelaort.models.servers.Server;
import art.aelaort.s3.ServersManagementS3;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ServersManagementService {
	private final ServersManagementS3 serversManagementS3;
	private final JsonMapper jsonMapper;
	@Value("${servers.management.json_path}")
	private Path jsonDataPath;

	public void saveIps(List<Server> servers) {
		String text = servers.stream()
							  .filter(Server::isMonitoring)
							  .map(server -> server.getName() + ":" + server.getIp() + ":" + server.getPort())
							  .collect(Collectors.joining("\n"))
					  + "\n";
		serversManagementS3.uploadIps(text);
		log("ips uploaded");
	}

	public void saveData(List<Server> servers) {
		String json = toJson(servers);
		saveJsonToLocal(json);
		log("saved data to local");
		serversManagementS3.uploadData(json);
		log("saved data to s3");
	}

	@SneakyThrows
	private String toJson(List<Server> server) {
		return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(server);
	}

	@SneakyThrows
	private void saveJsonToLocal(String jsonStr) {
		Files.writeString(jsonDataPath, jsonStr);
	}
}
