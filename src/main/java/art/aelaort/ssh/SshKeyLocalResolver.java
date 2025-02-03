package art.aelaort.ssh;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class SshKeyLocalResolver {
	@Value("${ssh.keys.generated.dir}")
	private Path keysDir;

	public Path getPublicKeyPath(String title) {
		return keysDir.resolve(title + ".pub");
	}

	public Path getPrivateKeyPath(String title) {
		return keysDir.resolve(title);
	}
}
