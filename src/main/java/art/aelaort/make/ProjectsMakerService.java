package art.aelaort.make;

import art.aelaort.exceptions.ProjectAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ProjectsMakerService {
	private final PlaceholderFiller placeholderFiller;
	private final FillerProperties fillerProperties;
	@Value("${build.main.src.dir}")
	private Path mainSrcDir;
	@Value("${build.main.default_files.dir}")
	private Path defaultFilesDir;

	public boolean hasGit(String[] args) {
		return Arrays.stream(args).noneMatch(arg -> arg.equals("no-git"));
	}

	public boolean hasJooq(String[] args) {
		return Arrays.asList(args).contains("jooq");
	}

	public void makeJavaProject(String name, boolean hasGit, boolean hasJooq) {
		Path dir = mkdirForJava("java", name);
		ProjectMaker projectMaker = ProjectMaker.builder()
				.name(splitName(name))
				.hasGit(hasGit)
				.hasJooq(hasJooq)
				.build();

		generateMavenFile(dir, projectMaker);
		generateGitignoreFile(dir);
		generateJooqFile(dir, projectMaker);

		createSubDirectories(dir);

		generateClassFile(getClassDir(dir), projectMaker);
//		generatePropertiesFile();
	}

	private String splitName(String name) {
		String[] split = name.split("/");
		return split[split.length - 1];
	}

	@SneakyThrows
	private void createSubDirectories(Path dir) {
		Files.createDirectories(getClassDir(dir));
		Files.createDirectories(dir
				.resolve("src")
				.resolve("main")
				.resolve("resources"));
		Files.createDirectories(dir
				.resolve("src")
				.resolve("test")
				.resolve("main"));
	}

	private Path getClassDir(Path dir) {
		Path result = dir
				.resolve("src")
				.resolve("main")
				.resolve("java");
		for (String path : fillerProperties.getValue().getMainPackage().split("\\.")) {
			result = result.resolve(path);
		}
		return result;
	}

	private void generateJooqFile(Path dir, ProjectMaker projectMaker) {
		if (projectMaker.isHasJooq()) {
			String fileContent = getFileContent(fillerProperties.getJooqFile());
			String filled = placeholderFiller.fillFile(fileContent, projectMaker);
			writeFile(dir, filled, fillerProperties.getJooqFile());
		}
	}

	private void generateGitignoreFile(Path dir) {
		writeFile(dir, getFileContent(fillerProperties.getGitignoreFile()), fillerProperties.getGitignoreFile());
	}

	private void generateClassFile(Path dir, ProjectMaker projectMaker) {
		String fileContent = getFileContent(fillerProperties.getClassFile());
		String filled = placeholderFiller.fillFile(fileContent, projectMaker);
		writeFile(dir, filled, fillerProperties.getClassFile());
	}

	private void generateMavenFile(Path dir, ProjectMaker projectMaker) {
		String pomFileContent = getFileContent(fillerProperties.getPomFilepath());
		String filled = placeholderFiller.fillFile(pomFileContent, projectMaker);
		writeFile(dir, filled, fillerProperties.getPomFilepath());
	}

	@SneakyThrows
	private void writeFile(Path dir, String content, String filepath) {
		Files.writeString(dir.resolve(filepath), content);
	}

	@SneakyThrows
	private String getFileContent(String filepath) {
		return Files.readString(defaultFilesDir.resolve(filepath));
	}

	@SneakyThrows
	private Path mkdirForJava(String subdir, String name) {
		Path path = mainSrcDir.resolve(subdir).resolve(name);
		if (!Files.exists(path)) {
			Files.createDirectories(path);
			log("created dir: " + path);
			return path;
		} else {
			throw new ProjectAlreadyExistsException(path);
		}
	}
}
