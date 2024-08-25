package art.aelaort.make;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class Project {
	private String name;
	private boolean hasGit;
	private boolean hasJooq;
}
