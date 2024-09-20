package art.aelaort.docker;

import art.aelaort.ServersManagementService;
import art.aelaort.mappers.DockerMapper;
import art.aelaort.models.servers.Server;
import art.aelaort.models.ssh.SshServer;
import art.aelaort.ssh.SshClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapBlue;
import static art.aelaort.utils.ColoredConsoleTextUtils.wrapGreen;
import static java.util.stream.Collectors.joining;

@Component
@RequiredArgsConstructor
public class DockerStatsService {
	private final SshClient sshClient;
	private final ServersManagementService serversManagementService;
	private final DockerMapper dockerMapper;

	public String statAllServers() {
		String statsCommand = "docker stats --no-stream --format \"table {{.Name}}\\t{{.CPUPerc}}\\t{{.MemUsage}}\\t{{.MemPerc}}\\t{{.NetIO}}\"";
		String dfhCommand = "df -h";
		String splitRow = "\n" + "=".repeat(100) + "\n";
		return serversManagementService.scanOnlyLocalData()
				.stream()
				.filter(this::hasDockerService)
				.map(dockerMapper::map)
				.map(sshServer -> prettyStdoutExecCommandsOnServer(sshServer, statsCommand, dfhCommand))
				.collect(joining(splitRow));
	}

	private String prettyStdoutExecCommandsOnServer(SshServer server, String statsCommand, String dfhCommand) {
		String statsResult = sshClient.getCommandStdout(statsCommand, server);
		String dfhResult = sshClient.getCommandStdout(dfhCommand, server);
		return """
				%s:
				%s
				%s
				
				%s
				%s
				"""
				.formatted(
						wrapGreen(server.serverDirName()),
						wrapBlue("docker stats"),
						statsResult.trim(),
						wrapBlue("df -h"),
						filterDockerStatsOutput(dfhResult).trim()
				);
	}

	private boolean hasDockerService(Server server) {
		return server.getServices()
				.stream()
				.anyMatch(serviceDto -> serviceDto.getYmlName().startsWith("docker"));
	}

	private String filterDockerStatsOutput(String dockerStats) {
		return Stream.of(dockerStats.split("\n"))
				.filter(row -> !row.contains("/var/lib/docker/"))
				.collect(joining("\n"));
	}
}
