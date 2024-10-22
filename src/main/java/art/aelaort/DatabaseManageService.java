package art.aelaort;

import art.aelaort.utils.system.Response;
import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class DatabaseManageService {
	private final SystemProcess systemProcess;

	@Value("${db.local.docker_compose.path}")
	private String dbLocalDockerComposePath;
	@Value("${db.local.migrations.dir}")
	private Path dbLocalMigrationsDir;
	@Value("${db.local.migrations.scripts}")
	private String[] dbLocalMigrationsScripts;

	@Value("${db.remote.ssh.docker_compose.path}")
	private String dbRemoteSshDockerComposePath;
	@Value("${db.remote.ssh.port}")
	private String dbRemoteSshPort;

	@Value("${db.remote.migrations.dir}")
	private Path dbRemoteMigrationsDir;
	@Value("${db.remote.migrations.status}")
	private String dbRemoteMigrationsStatus;
	@Value("${db.remote.migrations.scripts}")
	private String[] dbRemoteMigrationsScripts;

	public void remoteStatus() {
		sshUp();
		String command = dbRemoteMigrationsDir.resolve(dbRemoteMigrationsStatus).toString();
		systemProcess.callProcessInheritIO(command, dbRemoteMigrationsDir);
		sshDown();
	}

	public void remoteUpdate() {
		sshUp();
		for (String script : dbRemoteMigrationsScripts) {
			String command = dbRemoteMigrationsDir.resolve(script).toString();
			systemProcess.callProcessInheritIO(command, dbRemoteMigrationsDir);
		}
		sshDown();
	}

	private void sshUp() {
		String command = "docker compose -f %s up -d --build".formatted(dbRemoteSshDockerComposePath);
		systemProcess.callProcessInheritIO(command);
		log("SSH tunnel (%s) - started%n", dbRemoteSshPort);
	}

	private void sshDown() {
		String command = "docker compose -f %s down".formatted(dbRemoteSshDockerComposePath);
		systemProcess.callProcessInheritIO(command);
		log("SSH tunnel - stopped");
	}

	public boolean isLocalRunning() {
		String command = "docker compose -f %s ps -q".formatted(dbLocalDockerComposePath);
		Response response = systemProcess.callProcess(command);
		if (response.exitCode() == 0) {
			return !response.stdout().trim().isEmpty();
		}

		throw new RuntimeException(response.stderr());
	}

	public void localUp() {
		String command = "docker compose -f %s up -d --build".formatted(dbLocalDockerComposePath);
		systemProcess.callProcessInheritIO(command);
		log("PostgreSQL (5433) - started");

		sleep();
		for (String script : dbLocalMigrationsScripts) {
			systemProcess.callProcessInheritIO(dbLocalMigrationsDir.resolve(script).toString(), dbLocalMigrationsDir);
		}
		log("Migrations - executed");
	}

	public void localDown() {
		String command = "docker compose -f %s down".formatted(dbLocalDockerComposePath);
		systemProcess.callProcessInheritIO(command);
		log("PostgreSQL (5433) - stopped");
	}

	@SneakyThrows
	private void sleep() {
		TimeUnit.SECONDS.sleep(1);
	}
}
