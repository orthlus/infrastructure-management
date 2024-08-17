package art.aelaort;

import art.aelaort.utils.system.Response;
import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class GitStatService {
	private final SystemProcess systemProcess;
	@Value("${build.main.src.dir}")
	private String srcRootDir;

	public String readStatWithInterval(String interval) {
		if (Set.of("day", "week", "month", "year").contains(interval)) {
			return readStat(interval);
		}

		throw new IllegalStateException("Unexpected value: " + interval);
	}

	public String readStatForDay() {
		return readStat("day");
	}

	private String readStat(String interval) {
		try {
			List<Path> dirs = Files.walk(Path.of(srcRootDir), 2)
					.filter(path -> path.toFile().isDirectory())
					.filter(path -> path.resolve(".git").toFile().exists())
					.toList();

			StringBuilder sb = new StringBuilder("git stat 1 %s ago:\n".formatted(interval));
			for (Path dir : dirs) {
				String stat = readStat(dir, interval);
				if (!stat.isEmpty()) {
					sb.append(dir)
							.append(" -")
							.append(stat)
							.append("\n");
				}
			}

			return sb.deleteCharAt(sb.length() - 1).toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String readStat(Path dir, String interval) {
		Response response = systemProcess.callProcess(dir, "git diff --shortstat \"@{1 %s ago}\" --cached".formatted(interval));
		if (response.exitCode() == 0) {
			return response.stdout();
		}

		throw new RuntimeException(response.stdout());
	}
}
