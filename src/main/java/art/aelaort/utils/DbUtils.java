package art.aelaort.utils;

import art.aelaort.properties.DbManageProperties;
import com.google.common.io.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DbUtils {
	private final DbManageProperties props;

	public String getName(String[] args) {
		if (args == null) {
			return props.getDefaultName();
		}
		if (args.length > 1) {
			return args[1];
		}
		return props.getDefaultName();
	}

	public List<String> getDbFilesOrder(Path dir) {
		try {
			return Files.readLines(dir.resolve("files.txt").toFile(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Path getDbDir(String name, String stand) {
		return props.getMigrationsDir()
				.resolve(name)
				.resolve(stand);
	}
}
