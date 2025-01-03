package art.aelaort.models.servers;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Builder(toBuilder = true)
@Getter
@Accessors(fluent = true)
public class DirServer {
	private String name;
	private boolean monitoring;
	private List<ServiceDto> services;
}
