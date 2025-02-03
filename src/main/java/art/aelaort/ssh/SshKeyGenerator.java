package art.aelaort.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class SshKeyGenerator {
	private final SshKeyLocalResolver sshKeyLocalResolver;
	@Value("${ssh.keys.generated.default-comment}")
	private String defaultComment;

	public void generateKey(String[] args) {
		if (args.length == 1) {
			generateKey(args[0], defaultComment);
		} else if (args.length >= 2) {
			generateKey(args[0], args[1]);
		}
	}

	public void generateKey(String title, String comment) {
		Path privateKeyPath = sshKeyLocalResolver.getPrivateKeyPath(title);
		Path publicKeyPath = sshKeyLocalResolver.getPublicKeyPath(title);
		try {
			JSch jsch = new JSch();
			KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 4096);
			keyPair.writePrivateKey(privateKeyPath.toString());
			keyPair.writePublicKey(publicKeyPath.toString(), comment);
			keyPair.dispose();

			log("saved in " + privateKeyPath);
			log();
			log(Files.readString(publicKeyPath).strip());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
