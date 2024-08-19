package art.aelaort.models.build;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Job {
	@JsonProperty
	private int id;
	@JsonProperty
	private String name;
	@JsonProperty("build_type")
	private BuildType buildType;
	@JsonProperty("sub_directory")
	private String subDirectory;
	@JsonProperty("project_dir")
	private String projectDir;
	@JsonProperty("secrets_directory")
	private String secretsDirectory;
	@JsonProperty
	private boolean db;

	@Override
	public String toString() {
		return "Job{" +
				"id=" + id +
				", name='" + name + '\'' +
				", buildType=" + buildType +
				", subDirectory='" + subDirectory + '\'' +
				", projectDir='" + projectDir + '\'' +
				", secretsDirectory='" + secretsDirectory + '\'' +
				'}';
	}
}
