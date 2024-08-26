package art.aelaort.make;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@Builder
@AllArgsConstructor
public class Project {
	@With
	private String name;
	private Integer id;
	private boolean hasGit;
	@With
	private boolean hasJooq;
}
