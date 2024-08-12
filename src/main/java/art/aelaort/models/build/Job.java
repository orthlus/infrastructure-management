package art.aelaort.models.build;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
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
	@Nullable
	private String projectDir;
	@Nullable
	private String secretsDirectory;

	@JsonProperty("project_dir")
	public void setProjectDir(String projectDir) {
		this.projectDir = projectDir.equals("None") ? null : projectDir;
	}

	@JsonProperty("secrets_directory")
	public void setSecretsDirectory(String secretsDirectory) {
		this.projectDir = secretsDirectory.equals("None") ? null : secretsDirectory;
	}

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
