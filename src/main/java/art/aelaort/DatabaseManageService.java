package art.aelaort;

import art.aelaort.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

import static java.nio.file.Path.of;

@Component
@RequiredArgsConstructor
public class DatabaseManageService {
	private final SystemProcess systemProcess;

	@Value("${db.local.docker_compose.path}")
	private String dbLocalDockerComposePath;
	@Value("${db.local.migrations.dir}")
	private String dbLocalMigrationsDir;
	@Value("${db.local.migrations.scripts}")
	private String[] dbLocalMigrationsScripts;

	public void localUp() {
		systemProcess.callProcessInheritIO("docker compose -f %s up -d --build".formatted(dbLocalDockerComposePath));
		System.out.println("PostgreSQL (5433) - started");

		for (String script : dbLocalMigrationsScripts) {
			Path dir = of(dbLocalMigrationsDir);
			systemProcess.callProcessInheritIO(dir.resolve(script).toString(), dir);
		}
		System.out.println("Migrations - executed");
	}

	public void localDown() {
		systemProcess.callProcessInheritIO("docker compose -f %s down".formatted(dbLocalDockerComposePath));
		System.out.println("PostgreSQL (5433) - stopped");
	}
}
