package art.aelaort.db;

import liquibase.integration.commandline.LiquibaseCommandLine;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class LiquibaseService {
	public boolean statusCli(Path file, String url) {
		String[] args = ArrayUtils.add(baseArgs(file, url), "status");
		int execute = new LiquibaseCommandLine().execute(args);
		if (execute == 0) {
			log("migration success");
			return true;
		} else {
			log("migration error");
			return false;
		}
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean updateCli(Path file, String url) {
		String[] args = ArrayUtils.add(baseArgs(file, url), "update");
		int execute = new LiquibaseCommandLine().execute(args);
		if (execute == 0) {
			log("migration success");
			return true;
		} else {
			log("migration error");
			return false;
		}
	}

	private String[] baseArgs(Path file, String url) {
		return new String[]{
				"--changelog-file=" + file.getFileName(),
				"--searchPath=" + file.getParent(),
				"--url=" + url,
				"--driver=org.postgresql.Driver"
		};
	}
}
