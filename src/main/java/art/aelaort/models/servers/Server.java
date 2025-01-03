package art.aelaort.models.servers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.join;

@Builder
@Getter
@Jacksonized
public class Server {
	@JsonProperty
	@With
	private Integer id;
	@JsonProperty
	private String name;
	@JsonProperty
	private String ip;
	@JsonProperty
	private String sshKey;
	@JsonProperty
	private Integer port;
	@JsonProperty
	private boolean monitoring;
	@JsonProperty
	private List<ServiceDto> services;
	@JsonProperty
	private Integer price;

	public static String servicesStr(List<ServiceDto> services) {
		return join(", ", services.stream()
				.map(s -> s.getDockerName() != null ? s.getDockerName() : s.getService())
				.toList());
	}

	public static List<Server> addNumbers(List<Server> servers) {
		AtomicInteger i = new AtomicInteger(1);
		return servers.stream()
				.sorted(Comparator.comparing(Server::getName))
				.map(server -> server.withId(i.getAndIncrement()))
				.toList();
	}
}
