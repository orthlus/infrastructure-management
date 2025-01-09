package art.aelaort.db;

import art.aelaort.properties.DbManageProperties;
import art.aelaort.utils.DbUtils;
import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class RemoteDb {
	private final SystemProcess systemProcess;
	private final DbManageProperties props;
	private final DbUtils dbUtils;

	public void remoteStatus(String[] args) {
		Path dir = dbUtils.getDbDir(dbUtils.getName(args), "prod");
		sshUp(dir);
		String command = dir.resolve(props.getStatusFilename()).toString();
		systemProcess.callProcessInheritIO(command, dir);
		sshDown(dir);
	}

	public void remoteRun(String[] args) {
		Path dir = dbUtils.getDbDir(dbUtils.getName(args), "prod");
		sshUp(dir);
		for (String script : dbUtils.getDbFilesOrder(dir)) {
			systemProcess.callProcessInheritIO(dir.resolve(script).toString(), dir);
		}
		sshDown(dir);
	}

	private void sshUp(Path dir) {
		String command = "docker compose -f %s up -d --build"
				.formatted(dir.resolve(props.getRemoteSshDockerComposeFilename()));
		systemProcess.callProcessInheritIO(command);
		log("SSH tunnel - started");
	}

	private void sshDown(Path dir) {
		String command = "docker compose -f %s down"
				.formatted(dir.resolve(props.getRemoteSshDockerComposeFilename()));
		systemProcess.callProcessInheritIO(command);
		log("SSH tunnel - stopped");
	}
}
