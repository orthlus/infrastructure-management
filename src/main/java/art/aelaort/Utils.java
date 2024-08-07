package art.aelaort;

import java.nio.file.Path;

public class Utils {
	public static String linuxResolve(String root, Path path) {
		return root + "/" + path.toString();
	}

	public static String linuxResolve(String root, String path) {
		return root + "/" + path;
	}
}
