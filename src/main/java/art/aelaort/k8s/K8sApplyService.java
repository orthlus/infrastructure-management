package art.aelaort.k8s;

import art.aelaort.exceptions.ExitWithUsageException;
import art.aelaort.utils.system.Response;
import art.aelaort.utils.system.SystemProcess;
import com.google.common.io.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static art.aelaort.utils.ColoredConsoleTextUtils.*;
import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class K8sApplyService {
	private final K8sProps k8sProps;
	private final SystemProcess systemProcess;
	private final String unchanged = " unchanged";
	private final String configured = " configured";

	private Boolean filterOutput(String stdout) {
		return !stdout.contains(unchanged) && !stdout.contains(configured);
	}

	public void apply(String[] args) {
		try {
			apply0(args);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void apply0(String[] args) throws IOException {
		List<String> clustersNames = Files.readLines(k8sProps.getClustersListFile().toFile(), StandardCharsets.UTF_8);

		if (args.length < 2) {
			log("k8s apply required id of cluster:");
			clustersNames.forEach(message -> log(wrapGreen(message)));
			throw new ExitWithUsageException();
		}

		boolean dry;
		if (args[1].equals("dry")) {
			dry = true;
		} else if (args[1].equals("run")) {
			dry = false;
		} else {
			throw new ExitWithUsageException();
		}
		boolean prune = args.length >= 3 && args[2].equals("del");

		int clusterN = Integer.parseInt(args[0]) - 1;
		String clusterName = clustersNames.get(clusterN);
		Path config = k8sProps.getDir().resolve(clusterName).resolve(k8sProps.getConfigPath());
		Path yamlDir = k8sProps.getDir().resolve(clusterName).resolve(k8sProps.getPathFiles());
		String command = command(config, yamlDir, dry, prune);
		log(command);
		log();

		Response response = systemProcess.callProcessInheritFilteredStdout(this::filterOutput, command);
		log();
		printSecrets(response.stdout());
		log();
		if (response.exitCode() != 0) {
			log(wrapRed("k8s apply finished with error"));
			log(wrapRed(response.stderr()));
		}
	}

	private void printSecrets(String stdout) {
		String[] lines = stdout.split("\n");
		for (String line : lines) {
			if (line.startsWith("secret/") && !line.contains(unchanged)) {
				String colored = line.replace(configured, wrapBlue(configured));
				log(colored);
			}
		}
	}

	private String command(Path config, Path yamlDir, boolean dry, boolean prune) {
		StringBuilder sb = new StringBuilder("kubectl --kubeconfig ");
		sb.append(config.toAbsolutePath());
		sb.append(" apply ");
		if (dry) {
			sb.append(" --dry-run=server ");
		}
		if (prune) {
			sb.append(" --prune --all ");
		}
		sb.append(" -R -f ").append(yamlDir.toAbsolutePath());

		return sb.toString();
	}
}
