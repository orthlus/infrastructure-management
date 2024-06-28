package art.aelaort;

import art.aelaort.models.PhysicalServer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SerializeService {
	private final ObjectMapper mapper;

	public List<PhysicalServer> serversParse(String json) {
		try {
			return List.of(mapper.readValue(json, PhysicalServer[].class));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public String toJson(List<PhysicalServer> physicalServer) {
		try {
			return mapper.writeValueAsString(physicalServer);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
