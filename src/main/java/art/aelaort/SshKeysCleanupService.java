package art.aelaort;

import art.aelaort.build.JobsProvider;
import art.aelaort.models.build.Job;
import art.aelaort.models.servers.Server;
import art.aelaort.servers.providers.ServerProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class SshKeysCleanupService {
	private final ServerProvider serverProvider;
	private final JobsProvider jobsProvider;
	@Value("${ssh.cleanup.hostings-file}")
	private Path hostingsFile;
	@Value("${ssh.keys.dir}")
	private Path keysDir;

	public void cleanAll() {
		cleanLocal();
	}

	public void cleanLocal() {
		Set<String> servers = getServers();
		Set<String> jobs = getJobs();

		try {
			List<Path> toDelete = new ArrayList<>(30);
			for (String dir : Files.readAllLines(hostingsFile)) {
				try (Stream<Path> list = Files.list(keysDir.resolve(dir))) {
					list
							.filter(Files::isRegularFile)
							.map(Path::getFileName)
							.map(Path::toString)
							.forEach(file -> {
								if (file.endsWith(".pub")) {
									String noSuffix = file.replaceAll("\\.pub$", "");
									if (!jobs.contains(noSuffix) && !servers.contains(noSuffix)) {
										toDelete.add(keysDir.resolve(dir).resolve(file));
									}
								} else {
									if (!jobs.contains(file) && !servers.contains(file)) {
										toDelete.add(keysDir.resolve(dir).resolve(file));
									}
								}
							});
				}
			}
			for (Path s : toDelete) {
				Files.deleteIfExists(s);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Set<String> getServers() {
		return serverProvider.readLocalJsonData()
				.stream().map(Server::getName).collect(Collectors.toSet());
	}

	private Set<String> getJobs() {
		return jobsProvider.readBuildConfig()
				.stream().map(Job::getName).collect(Collectors.toSet());
	}
}
