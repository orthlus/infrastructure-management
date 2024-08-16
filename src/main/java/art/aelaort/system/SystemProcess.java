package art.aelaort.system;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Component
public class SystemProcess {
	private final Set<String> knownCommands = Set.of("mvn", "yarn", "docker");
	private final Set<String> cmdCommands = Set.of("mvn", "yarn");

	private String fixCmdCall(String command) {
		String first = StringUtils.split(command)[0];
		if (cmdCommands.contains(first)) {
			return "cmd /c " + command;
		}
		if (knownCommands.contains(first)) {
			return command;
		}

		Response response = callProcess("where /f " + first);
		if (response.exitCode() == 0) {
			String[] split = response.stdout().split("\"");
			if (split.length >= 1) {
				if (matchExt(split, ".exe")) {
					return command;
				} else if (matchExt(split, ".cmd")) {
					return "cmd /c " + command;
				}
			}
		}
		return command;
	}

	private boolean matchExt(String[] paths, String extension) {
		return Stream.of(paths).anyMatch(s -> s.endsWith(extension));
	}

	public void callProcessForBuild(String command, Path dir) {
		try {
			String[] commandArray = StringUtils.split(fixCmdCall(command));
			Process p = new ProcessBuilder(commandArray)
					.inheritIO()
					.directory(dir.toFile())
					.start();
			p.waitFor();
		} catch (Exception e) {
			System.err.println("java system process call error: " + e.getLocalizedMessage());
			System.exit(1);
		}
	}

	public void callProcessInheritIO(String command, Path dir) {
		try {
			String[] commandArray = StringUtils.split(command);
			Process p = new ProcessBuilder(commandArray)
					.inheritIO()
					.directory(dir.toFile())
					.start();
			p.waitFor();
		} catch (Exception e) {
			System.err.println("java system process call error: " + e.getLocalizedMessage());
			System.exit(1);
		}
	}

	public void callProcessInheritIO(String command) {
		try {
			String[] commandArray = StringUtils.split(command);
			Process p = new ProcessBuilder(commandArray)
					.inheritIO()
					.start();
			p.waitFor();
		} catch (Exception e) {
			System.err.println("java system process call error: " + e.getLocalizedMessage());
			System.exit(1);
		}
	}

	public Response callProcess(String... command) {
		return callProcess(null, command);
	}

	public Response callProcess(Path dir, String... command) {
		try {
			ProcessBuilder pb;
			if (command.length == 1) {
				String[] split = StringUtils.split(command[0]);
				pb = new ProcessBuilder(split);
			} else {
				pb = new ProcessBuilder(command);
			}

			if (dir != null) {
				pb.directory(dir.toFile());
			}

			Process p = pb.start();
			p.waitFor(30, TimeUnit.MINUTES);

			StringBuilder stdout = new StringBuilder();
			try (BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = b.readLine()) != null) {
					stdout.append(line);
				}
			}

			StringBuilder stderr = new StringBuilder();
			try (BufferedReader b = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
				String line;
				while ((line = b.readLine()) != null) {
					stderr.append(line);
				}
			}

			return new Response(p.exitValue(), stdout.toString(), stderr.toString());
		} catch (Exception e) {
			e.printStackTrace();
			return new Response(1, "thrown exception in java", e.getMessage());
		}
	}
}
