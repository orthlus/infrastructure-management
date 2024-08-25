package art.aelaort.make;

public class ProjectMapper {
	public static Project map(Project project, String name) {
		return Project.builder()
				.name(name)
				.hasJooq(project.isHasJooq())
				.hasGit(project.isHasGit())
				.id(project.getId())
				.build();
	}
}
