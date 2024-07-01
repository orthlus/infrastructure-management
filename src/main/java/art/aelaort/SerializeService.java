package art.aelaort;

import art.aelaort.models.Server;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SerializeService {
	private final ObjectMapper mapper;

	public List<Server> serversParse(String json) {
		try {
			return List.of(mapper.readValue(json, Server[].class));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public String toJson(List<Server> server) {
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(server);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
