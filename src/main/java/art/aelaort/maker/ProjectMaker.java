package art.aelaort.maker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProjectMaker {
	private String name;
	private boolean hasGit;
	private boolean hasJooq;
}
