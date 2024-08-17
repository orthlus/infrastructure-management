package art.aelaort.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static java.nio.file.Path.of;

@Component
public class Utils {
	@Value("${tmp.root.dir}")
	private String tmpRootDir;

	public Path createTmpDir() {
		return createTmpDir(tmpRootDir);
	}

	public static Path createTmpDir(String tmpRootDir) {
		try {
			Path path = of(tmpRootDir).resolve(UUID.randomUUID().toString());
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
}
