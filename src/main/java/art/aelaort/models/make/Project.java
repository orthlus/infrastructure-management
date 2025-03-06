package art.aelaort.models.make;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Project {
	private String name;
	private Integer id;
	private boolean hasGit;
	private boolean hasJooq;
	private boolean isMavenBuildForLocal;
	private String dir;
}
