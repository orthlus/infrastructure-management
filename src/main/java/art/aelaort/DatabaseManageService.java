package art.aelaort;

import art.aelaort.utils.system.Response;
import art.aelaort.utils.system.SystemProcess;
import com.google.common.io.Files;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
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

	@Value("${db.remote.ssh.docker_compose.filename}")
	private String dbSshDockerComposeFile;

	@Value("${db.remote.migrations.dir}")
	private Path dbRemoteMigrationsDir;
	@Value("${db.remote.migrations.status}")
	private String dbRemoteMigrationsStatus;

	public void remoteStatus() {
		sshUp();
		String command = dbRemoteMigrationsDir.resolve(dbRemoteMigrationsStatus).toString();
		systemProcess.callProcessInheritIO(command, dbRemoteMigrationsDir);
		sshDown();
	}

	public void remoteUpdate() {
		sshUp();
		for (String script : getFilesOrder(dbRemoteMigrationsDir)) {
			String command = dbRemoteMigrationsDir.resolve(script).toString();
			systemProcess.callProcessInheritIO(command, dbRemoteMigrationsDir);
		}
		sshDown();
	}

	private List<String> getFilesOrder(Path dir) {
		try {
			return Files.readLines(dir.resolve("order.txt").toFile(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void sshUp() {
		String command = "docker compose -f %s up -d --build".formatted(dbRemoteMigrationsDir.resolve(dbSshDockerComposeFile));
		systemProcess.callProcessInheritIO(command);
		log("SSH tunnel - started");
	}

	private void sshDown() {
		String command = "docker compose -f %s down".formatted(dbRemoteMigrationsDir.resolve(dbSshDockerComposeFile));
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
		String command = "docker run -d -p 5433:5432 --env POSTGRES_PASSWORD=postgres --name pg-tmp-dev postgres:16";
		systemProcess.callProcessInheritIO(command);
		log("PostgreSQL (5433) - started");

		sleep();
		for (String script : getFilesOrder(dbLocalMigrationsDir)) {
			systemProcess.callProcessInheritIO(dbLocalMigrationsDir.resolve(script).toString(), dbLocalMigrationsDir);
		}
		log("Migrations - executed");
	}

	public void localDown() {
		String command = "docker rm -f -v pg-tmp-dev";
		systemProcess.callProcessInheritIO(command);
		log("PostgreSQL (5433) - stopped");
	}

	@SneakyThrows
	private void sleep() {
		TimeUnit.SECONDS.sleep(1);
	}
}
