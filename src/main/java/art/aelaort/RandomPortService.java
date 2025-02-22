package art.aelaort;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.unix4j.Unix4j;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class RandomPortService {
	@Value("${port.scanning.root}")
	private Path root;
	@Value("${port.scanning-dirs}")
	private List<String> dirs;

	private final Random random = new Random();
	private final int minPort = 10000;
	private final int maxPort = 65535;

	public String getRandomPort() {
		int port = random.nextInt(minPort, maxPort);
		while (isPortUsed(port)) {
			port = random.nextInt(minPort, maxPort);
		}
		return String.valueOf(port);
	}

	private boolean isPortUsed(int port) {
		return !Unix4j.grep(String.valueOf(port), files())
				.toLineList()
				.isEmpty();
	}

	private File[] files() {
		return dirs.stream()
				.flatMap(subDir -> files(subDir).stream())
				.toArray(File[]::new);
	}

	private Set<File> files(String subDir) {
		try (Stream<Path> walk = Files.walk(root.resolve(subDir))) {
			return walk
					.filter(p -> !p.toString().contains(".git"))
					.filter(p -> !p.toString().contains(".idea"))
					.filter(p -> !p.toString().endsWith(".csv"))
					.filter(Files::isRegularFile)
					.map(Path::toFile)
					.collect(Collectors.toSet());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void copyToClipboard(String s) {
		StringSelection stringSelection = new StringSelection(s);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}
}
