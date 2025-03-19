package art.aelaort.models.servers;

import lombok.Builder;
import lombok.Getter;
import lombok.With;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class K8sApp {
	private String name;
	@With
	private String image;
	private String kind;
	private String schedule;
}
