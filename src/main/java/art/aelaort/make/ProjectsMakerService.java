package art.aelaort.make;

import art.aelaort.BuildService;
import art.aelaort.exceptions.AppNotFoundException;
import art.aelaort.exceptions.InvalidAppParamsException;
import art.aelaort.exceptions.ProjectAlreadyExistsException;
import art.aelaort.models.build.BuildType;
import art.aelaort.models.build.Job;
import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ProjectsMakerService {
	private final PlaceholderFiller placeholderFiller;
	private final FillerMakeJavaProperties properties;
	private final SystemProcess systemProcess;
	private final BuildService buildService;
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

	public void makeJavaProject(String nameOrId, boolean hasGit, boolean hasJooq) {
		Project project = buildProject(nameOrId, hasGit, hasJooq);
		Path dir = mkdirForJava("java", project.getName());

		generateMavenFile(dir, project);
		generateGitignoreFile(dir);
		generateJooqFile(dir, project);

		createSubDirectories(dir);

		generateClassFile(getClassDir(dir), project);
		generatePropertiesFile(getResourcesDir(dir), project);
		generateGit(dir, project);
	}

	private Project buildProject(String nameOrId, boolean hasGit, boolean hasJooq) {
		Project.ProjectBuilder projectBuilder = Project.builder()
				.hasGit(hasGit)
				.hasJooq(hasJooq);
		try {
			int id = Integer.parseInt(nameOrId);
			return enrich(projectBuilder.id(id).build());
		} catch (NumberFormatException e) {
			return projectBuilder.name(nameOrId).build();
		}
	}

	private Project enrich(Project project) {
		if (project.getId() == null || project.getName() != null) {
			return project;
		}

		Job job = buildService.getJobsMapById().get(project.getId());
		if (job == null) {
			throw new AppNotFoundException(project);
		}

		Set<BuildType> javaTypes = Set.of(BuildType.java_local, BuildType.java_docker);
		if (job.getSubDirectory().equals("java") && javaTypes.contains(job.getBuildType())) {
			Project.ProjectBuilder newProjectBuilder = Project.builder()
					.name(job.getName())
					.hasJooq(job.isDb())
					.hasGit(project.isHasGit());

			if (job.getBuildType() == BuildType.java_local) {
				newProjectBuilder.isMavenBuildForLocal(true);
			}

			return newProjectBuilder.build();
		} else {
			throw new InvalidAppParamsException();
		}
	}

	private void generateGit(Path dir, Project project) {
		if (project.isHasGit()) {
			try {
				systemProcess.callProcessThrows(dir, "git init");
				systemProcess.callProcessThrows(dir, "git add .");
				systemProcess.callProcessThrows(dir, "git commit -m 'init'");
			} catch (RuntimeException e) {
				log("generate git error");
				throw new RuntimeException(e);
			}
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
		String fileContent = getFileContent(properties.getClassFile());
		String filled = placeholderFiller.fillFile(fileContent, project);
		writeFile(dir, filled, properties.getClassFile());
	}

	private void generateMavenFile(Path dir, Project project) {
		String filepath = project.isMavenBuildForLocal() ?
				properties.getPomForLocalFilepath() :
				properties.getPomFilepath();

		String pomFileContent = getFileContent(filepath);
		String filled = placeholderFiller.fillFile(pomFileContent, project);
		writeFile(dir, filled, filepath);
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
