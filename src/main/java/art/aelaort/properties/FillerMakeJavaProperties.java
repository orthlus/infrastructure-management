package art.aelaort.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("projects.make.java")
@Getter
@Setter
public class FillerMakeJavaProperties {
	private Placeholder placeholder;
	private Value value;
	private String jooqPluginFilepath;
	private String jooqDependencyFilepath;
	private String pomFilepath;
	private String classFile;
	private String jooqFile;
	private String gitignoreFile;
	private String propertiesFile;
	private String pomForLocalFilepath;
	private String classFileForLocalFilepath;
	private String pomDefaultName;
	private String classFileDefaultName;

	@Getter
	@Setter
	public static class Placeholder {
		private String groupId;
		private String groupVersion;
		private String springVersion;
		private String javaVersion;
		private String projectName;
		private String mainPackage;
		private String jooqPlugin;
		private String jooqDependency;
	}

	@Getter
	@Setter
	public static class Value {
		private String groupId;
		private String groupVersion;
		private String springVersion;
		private String javaVersion;
		private String mainPackage;
	}
}
