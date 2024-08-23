package art.aelaort.maker;

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
	@Value("${build.main.src.dir}")
	private Path mainSrcDir;
	@Value("${build.main.default_files.dir}")
	private Path defaultFilesDir;
	@Value("${projects.maker.maven.pom.filepath}")
	private String pomFilepath;
	@Value("${projects.maker.maven.class.file}")
	private String classFilepath;
	@Value("${projects.maker.maven.gitignore.file}")
	private String gitignoreFilepath;
	@Value("${projects.maker.maven.jooq.file}")
	private String jooqFilepath;
	@Value("${projects.maker.maven.main.package.value}")
	private String projectsMakerMavenMainPackageValue;

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
		generateJooqFile(dir);

		createSubDirectories(dir);

		generateClassFile(getClassDir(dir));
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
		for (String path : projectsMakerMavenMainPackageValue.split("\\.")) {
			result = result.resolve(path);
		}
		return result;
	}

	private void generateJooqFile(Path dir) {
		String pomFileContent = getFileContent(jooqFilepath);
		String filled = placeholderFiller.fillJooqFile(pomFileContent);
		writeFile(dir, filled, jooqFilepath);
	}

	private void generateGitignoreFile(Path dir) {
		writeFile(dir, getFileContent(gitignoreFilepath), gitignoreFilepath);
	}

	private void generateClassFile(Path dir) {
		String pomFileContent = getFileContent(classFilepath);
		String filled = placeholderFiller.fillClassFile(pomFileContent);
		writeFile(dir, filled, classFilepath);
	}

	private void generateMavenFile(Path dir, ProjectMaker projectMaker) {
		String pomFileContent = getFileContent(pomFilepath);
		String filled = placeholderFiller.fillPomFile(pomFileContent, projectMaker);
		writeFile(dir, filled, pomFilepath);
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
