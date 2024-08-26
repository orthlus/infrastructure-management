package art.aelaort.make;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@Builder
public class Project {
	@With
	private String name;
	private Integer id;
	private boolean hasGit;
	@With
	private boolean hasJooq;
	private boolean isMavenBuildForLocal;
}
