package art.aelaort.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

@Component
public class Utils {
	@Value("${tmp.root.dir}")
	private Path tmpRootDir;
	@Value("${servers.management.docker.image.pattern}")
	private String dockerImagePattern;
	private String[] dockerImagePatternSplit;

	@PostConstruct
	private void init() {
		dockerImagePatternSplit = dockerImagePattern.split("%%");
	}

	public Path createTmpDir() {
		return createTmpDir(tmpRootDir);
	}

	public static Path createTmpDir(Path tmpRootDir) {
		try {
			Path path = tmpRootDir.resolve(UUID.randomUUID().toString());
			Files.createDirectory(path);
			return path;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String dockerCommandTableFormat(String... keys) {
		String collect = Stream.of(keys)
				.map("{{.%s}}"::formatted)
				.collect(joining("\\t"));
		return "\"table %s\"".formatted(collect);
	}

	public static String linuxResolve(String root, Path path) {
		return root + "/" + path.toString();
	}

	public static String linuxResolve(String root, String path) {
		return root + "/" + path;
	}

	public static void log(String format, Object... args) {
		System.out.printf(format, args);
	}

	public static void log(String message) {
		System.out.println(message);
	}

	public static void log() {
		System.out.println();
	}

	public static String[] slice(String[] arr, int start) {
		return Arrays.copyOfRange(arr, start, arr.length);
	}

	public static String[] slice(String[] arr, int start, int end) {
		return Arrays.copyOfRange(arr, start, end);
	}

	public String dockerImageClean(String dockerImage) {
		return dockerImage
				.replace(dockerImagePatternSplit[0], "")
				.replace(dockerImagePatternSplit[1], "");
	}
}
