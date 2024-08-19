package art.aelaort;

import art.aelaort.utils.system.Response;
import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

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
		systemProcess.callProcessInheritIO(dbRemoteMigrationsDir.resolve(dbRemoteMigrationsStatus).toString(), dbRemoteMigrationsDir);
		sshDown();
	}

	public void remoteUpdate() {
		sshUp();
		for (String script : dbRemoteMigrationsScripts) {
			systemProcess.callProcessInheritIO(dbRemoteMigrationsDir.resolve(script).toString(), dbRemoteMigrationsDir);
		}
		sshDown();
	}

	private void sshUp() {
		systemProcess.callProcessInheritIO("docker compose -f %s up -d --build".formatted(dbRemoteSshDockerComposePath));
		System.out.printf("SSH tunnel (%s) - started%n", dbRemoteSshPort);
	}

	private void sshDown() {
		systemProcess.callProcessInheritIO("docker compose -f %s down".formatted(dbRemoteSshDockerComposePath));
		System.out.println("SSH tunnel - stopped");
	}

	public boolean isLocalRunning() {
		Response response = systemProcess.callProcess("docker compose -f %s ps -q".formatted(dbLocalDockerComposePath));
		if (response.exitCode() == 0) {
			return !response.stdout().trim().isEmpty();
		}

		throw new RuntimeException(response.stderr());
	}

	public void localUp() {
		systemProcess.callProcessInheritIO("docker compose -f %s up -d --build".formatted(dbLocalDockerComposePath));
		System.out.println("PostgreSQL (5433) - started");

		sleep();
		for (String script : dbLocalMigrationsScripts) {
			systemProcess.callProcessInheritIO(dbLocalMigrationsDir.resolve(script).toString(), dbLocalMigrationsDir);
		}
		System.out.println("Migrations - executed");
	}

	public void localDown() {
		systemProcess.callProcessInheritIO("docker compose -f %s down".formatted(dbLocalDockerComposePath));
		System.out.println("PostgreSQL (5433) - stopped");
	}

	@SneakyThrows
	private void sleep() {
		TimeUnit.SECONDS.sleep(1);
	}
}
