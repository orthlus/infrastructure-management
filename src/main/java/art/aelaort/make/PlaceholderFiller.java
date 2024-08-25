package art.aelaort.make;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class PlaceholderFiller {
	@Value("${build.main.default_files.dir}")
	private Path defaultFilesDir;
	private final FillerMakeJavaProperties properties;

	public String fillFile(String srcFileContent, ProjectMaker projectMaker) {
		FillerMakeJavaProperties.Placeholder placeholder = properties.getPlaceholder();
		FillerMakeJavaProperties.Value value = properties.getValue();
		return srcFileContent
				.replace(placeholder.getGroupId(), value.getGroupId())
				.replace(placeholder.getGroupVersion(), value.getGroupVersion())
				.replace(placeholder.getSpringVersion(), value.getSpringVersion())
				.replace(placeholder.getJavaVersion(), value.getJavaVersion())
				.replace(placeholder.getProjectName(), projectMaker.getName())
				.replace(placeholder.getMainPackage(), value.getMainPackage())
				.replace(placeholder.getJooqPlugin(), projectMaker.isHasJooq() ? readJooqPluginContent() : "");
	}

	@SneakyThrows
	private String readJooqPluginContent() {
		return Files.readString(defaultFilesDir.resolve(properties.getJooqPluginFilepath()));
	}
}
