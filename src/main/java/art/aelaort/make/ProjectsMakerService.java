package art.aelaort.make;

import art.aelaort.build.JobsProvider;
import art.aelaort.exceptions.AppNotFoundException;
import art.aelaort.exceptions.InvalidAppParamsException;
import art.aelaort.exceptions.ProjectAlreadyExistsException;
import art.aelaort.models.build.Job;
import art.aelaort.models.make.Project;
import art.aelaort.properties.FillerMakeJavaProperties;
import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ProjectsMakerService {
	private final PlaceholderFiller placeholderFiller;
	private final FillerMakeJavaProperties properties;
	private final SystemProcess systemProcess;
	private final JobsProvider jobsProvider;
	@Value("${build.main.src.dir}")
	private Path mainSrcDir;
	@Value("${build.main.default_files.dir}")
	private Path defaultFilesDir;

	public void makeJavaProject(String nameOrId) {
		Project project = buildProject(nameOrId);
		Path dir = mkdirForJava(project);

		generateMavenFile(dir, project);
		generateGitignoreFile(dir);
		generateJooqFile(dir, project);

		createSubDirectories(dir);

		generateClassFile(getClassDir(dir), project);
		generatePropertiesFile(getResourcesDir(dir), project);
		generateGit(dir);
	}

	private Project buildProject(String nameOrId) {
		Project.ProjectBuilder projectBuilder = Project.builder();
		int id = Integer.parseInt(nameOrId);
		return enrich(projectBuilder.id(id).build());
	}

	private Project enrich(Project project) {
		if (project.getId() == null) {
			return project;
		}

		Job job = jobsProvider.getJobsMapById().get(project.getId());
		if (job == null) {
			throw new AppNotFoundException(project);
		}

		Set<String> javaTypes = Set.of("java_local", "java_docker");
		if (job.getSubDirectory().startsWith("java") && javaTypes.contains(job.getBuildType())) {
			Project.ProjectBuilder newProjectBuilder = Project.builder()
					.name(job.getName())
					.hasJooq(job.isDb())
					.dir(job.getSubDirectory());

			if (job.getBuildType().equals("java_local")) {
				newProjectBuilder.isMavenBuildForLocal(true);
			}

			return newProjectBuilder.build();
		} else {
			throw new InvalidAppParamsException();
		}
	}

	private void generateGit(Path dir) {
		try {
			systemProcess.callProcessThrows(dir, "git init");
			systemProcess.callProcessThrows(dir, "git add .");
			systemProcess.callProcessThrows(dir, "git commit -m init");
		} catch (RuntimeException e) {
			log("generate git error");
			throw new RuntimeException(e);
		}
	}

	private void generatePropertiesFile(Path dir, Project project) {
		String fileContent = getFileContent(properties.getPropertiesFile());
		String filled = placeholderFiller.fillFile(fileContent, project);
		writeFile(dir, filled, properties.getPropertiesFile());
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
				.resolve("java")
				.resolve("main"));
	}

	private Path getResourcesDir(Path dir) {
		return dir
				.resolve("src")
				.resolve("main")
				.resolve("resources");
	}

	private Path getClassDir(Path dir) {
		Path result = dir
				.resolve("src")
				.resolve("main")
				.resolve("java");
		for (String path : properties.getValue().getMainPackage().split("\\.")) {
			result = result.resolve(path);
		}
		return result;
	}

	private void generateJooqFile(Path dir, Project project) {
		if (project.isHasJooq()) {
			String fileContent = getFileContent(properties.getJooqFile());
			String filled = placeholderFiller.fillFile(fileContent, project);
			writeFile(dir, filled, properties.getJooqFile());
		}
	}

	private void generateGitignoreFile(Path dir) {
		writeFile(dir, getFileContent(properties.getGitignoreFile()), properties.getGitignoreFile());
	}

	private void generateClassFile(Path dir, Project project) {
		String filepath = project.isMavenBuildForLocal() ?
				properties.getClassFileForLocalFilepath() :
				properties.getClassFile();

		String fileContent = getFileContent(filepath);
		String filled = placeholderFiller.fillFile(fileContent, project);
		writeFile(dir, filled, properties.getClassFileDefaultName());
	}

	private void generateMavenFile(Path dir, Project project) {
		String filepath = project.isMavenBuildForLocal() ?
				properties.getPomForLocalFilepath() :
				properties.getPomFilepath();

		String pomFileContent = getFileContent(filepath);
		String filled = placeholderFiller.fillFile(pomFileContent, project);
		writeFile(dir, filled, properties.getPomDefaultName());
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
	private Path mkdirForJava(Project project) {
		String[] splitDir = project.getDir().split("/");
		Path path = mainSrcDir;
		for (String s : splitDir) {
			path = path.resolve(s);
		}
		path = path.resolve(project.getName());
		if (!Files.exists(path)) {
			Files.createDirectories(path);
			log("created dir: " + path);
			return path;
		} else {
			throw new ProjectAlreadyExistsException(path);
		}
	}
}
