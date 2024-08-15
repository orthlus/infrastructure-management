package art.aelaort;

import art.aelaort.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

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

	@Value("${db.remote.ssh.docker_compose.path}")
	private String dbRemoteSshDockerComposePath;
	@Value("${db.remote.ssh.port}")
	private String dbRemoteSshPort;

	@Value("${db.remote.migrations.dir}")
	private String dbRemoteMigrationsDir;
	@Value("${db.remote.migrations.status}")
	private String dbRemoteMigrationsStatus;
	@Value("${db.remote.migrations.scripts}")
	private String[] dbRemoteMigrationsScripts;

	public void remoteStatus() {
		sshUp();
		Path dir = of(dbRemoteMigrationsDir);
		systemProcess.callProcessInheritIO(dir.resolve(dbRemoteMigrationsStatus).toString(), dir);
		sshDown();
	}

	public void remoteUpdate() {
		sshUp();
		Path dir = of(dbRemoteMigrationsDir);
		for (String script : dbRemoteMigrationsScripts) {
			systemProcess.callProcessInheritIO(dir.resolve(script).toString(), dir);
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

	public void localUp() {
		systemProcess.callProcessInheritIO("docker compose -f %s up -d --build".formatted(dbLocalDockerComposePath));
		System.out.println("PostgreSQL (5433) - started");

		sleep();
		Path dir = of(dbLocalMigrationsDir);
		for (String script : dbLocalMigrationsScripts) {
			systemProcess.callProcessInheritIO(dir.resolve(script).toString(), dir);
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
