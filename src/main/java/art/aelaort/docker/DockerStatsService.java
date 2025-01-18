package art.aelaort.docker;

import art.aelaort.mappers.ServerMapper;
import art.aelaort.models.servers.Server;
import art.aelaort.models.ssh.SshServer;
import art.aelaort.servers.providers.ServerProvider;
import art.aelaort.ssh.SshClient;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

import static art.aelaort.utils.ColoredConsoleTextUtils.wrapBlue;
import static art.aelaort.utils.ColoredConsoleTextUtils.wrapGreen;
import static art.aelaort.utils.Utils.dockerCommandTableFormat;
import static java.util.stream.Collectors.joining;

@Component
@RequiredArgsConstructor
public class DockerStatsService {
	private final SshClient sshClient;
	private final ServerProvider serverProvider;
	private final ServerMapper serverMapper;

	private final List<Command> commands = Lists.newArrayList(
			new Command("docker ps -a",
					"docker ps -a --format " + dockerCommandTableFormat("Names", "RunningFor", "State", "Status", "Ports", "Size", "Mounts")
			),
			new Command("docker stats",
					"docker stats --no-stream --format " + dockerCommandTableFormat("Name", "CPUPerc", "MemUsage", "MemPerc", "NetIO")
			),
			new Command("df -hT", "df -hT"),
			new Command("docker system df", "docker system df")
	);

	public String statByServer(SshServer server) {
		return prettyStdoutExecCommandsOnServer(server);
	}

	public String statAllServers() {
		String splitRow = "\n\n" + "=".repeat(100) + "\n";
		return serverProvider.scanOnlyLocalData()
				.stream()
				.filter(this::hasDockerService)
				.map(serverMapper::map)
				.map(this::prettyStdoutExecCommandsOnServer)
				.collect(joining(splitRow));
	}

	private String prettyStdoutExecCommandsOnServer(SshServer server) {
		String list = commands.stream()
				.map(command -> getCommandString(command, sshClient.getCommandStdout(command.command(), server)))
				.collect(joining("\n\n"));
		return "%s:\n%s".formatted(
				wrapGreen(server.serverDirName()),
				list
		);
	}

	private String getCommandString(Command command, String result) {
		return wrapBlue(command.title()) + "\n" + filterOutput(command, result).trim();
	}

	private boolean hasDockerService(Server server) {
		return server.getServices()
				.stream()
				.anyMatch(serviceDto -> serviceDto.getYmlName().startsWith("docker"));
	}

	private String filterOutput(Command command, String output) {
		return switch (command.title()) {
			case "df -hT" -> Stream.of(output.split("\n"))
					.filter(row -> !row.contains("/var/lib/docker/"))
					.filter(row -> !row.contains("tmpfs"))
					.collect(joining("\n"));
			default -> output;
		};
	}

	record Command(String title, String command) {
	}
}
