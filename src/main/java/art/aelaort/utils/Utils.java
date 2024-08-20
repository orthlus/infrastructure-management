package art.aelaort.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class Utils {
	@Value("${tmp.root.dir}")
	private Path tmpRootDir;

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
}
