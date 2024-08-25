package art.aelaort.make;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class PlaceholderFiller {
	@Value("${projects.maker.maven.group.id.placeholder}")
	private String projectsMakerMavenGroupIdPlaceholder;
	@Value("${projects.maker.maven.group.id.value}")
	private String projectsMakerMavenGroupIdValue;
	@Value("${projects.maker.maven.group.version.placeholder}")
	private String projectsMakerMavenGroupVersionPlaceholder;
	@Value("${projects.maker.maven.group.version.value}")
	private String projectsMakerMavenGroupVersionValue;
	@Value("${projects.maker.maven.spring.version.placeholder}")
	private String projectsMakerMavenSpringVersionPlaceholder;
	@Value("${projects.maker.maven.spring.version.value}")
	private String projectsMakerMavenSpringVersionValue;
	@Value("${projects.maker.maven.java.version.placeholder}")
	private String projectsMakerMavenJavaVersionPlaceholder;
	@Value("${projects.maker.maven.java.version.value}")
	private String projectsMakerMavenJavaVersionValue;
	@Value("${projects.maker.maven.project.name.placeholder}")
	private String projectsMakerMavenProjectNamePlaceholder;
	@Value("${projects.maker.maven.main.package.placeholder}")
	private String projectsMakerMavenMainPackagePlaceholder;
	@Value("${projects.maker.maven.main.package.value}")
	private String projectsMakerMavenMainPackageValue;
	@Value("${projects.maker.maven.jooq.plugin.placeholder}")
	private String projectsMakerMavenJooqPluginPlaceholder;

	@Value("${build.main.default_files.dir}")
	private Path defaultFilesDir;
	@Value("${projects.maker.maven.jooq.plugin.filepath}")
	private String projectsMakerMavenJooqPluginFilepath;

	public String fillClassFile(String srcFileContent) {
		return srcFileContent
				.replace(projectsMakerMavenMainPackagePlaceholder, projectsMakerMavenMainPackageValue);
	}

	public String fillJooqFile(String srcFileContent) {
		return srcFileContent
				.replace(projectsMakerMavenMainPackagePlaceholder, projectsMakerMavenMainPackageValue);
	}

	public String fillPomFile(String srcFileContent, ProjectMaker projectMaker) {
		return srcFileContent
				.replace(projectsMakerMavenGroupIdPlaceholder, projectsMakerMavenGroupIdValue)
				.replace(projectsMakerMavenGroupVersionPlaceholder, projectsMakerMavenGroupVersionValue)
				.replace(projectsMakerMavenSpringVersionPlaceholder, projectsMakerMavenSpringVersionValue)
				.replace(projectsMakerMavenJavaVersionPlaceholder, projectsMakerMavenJavaVersionValue)
				.replace(projectsMakerMavenProjectNamePlaceholder, projectMaker.getName())
				.replace(projectsMakerMavenMainPackagePlaceholder, projectsMakerMavenMainPackageValue)
				.replace(projectsMakerMavenJooqPluginPlaceholder, projectMaker.isHasJooq() ? readJooqPluginContent() : "");
	}

	@SneakyThrows
	private String readJooqPluginContent() {
		return Files.readString(defaultFilesDir.resolve(projectsMakerMavenJooqPluginFilepath));
	}
}
