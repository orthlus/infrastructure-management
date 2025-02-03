package art.aelaort.ssh;

import art.aelaort.ssh.tw.SshKeyPost;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Path;

import static art.aelaort.utils.Utils.log;
import static java.nio.file.Files.readString;

@Component
@RequiredArgsConstructor
public class SshKeyCloudUploader {
	private final SshKeyLocalResolver sshKeyLocalResolver;
	private final RestTemplate tw;

	public void uploadKey(String[] args) {
		if (args.length >= 1) {
			tw.postForObject("/v1/ssh-keys", getBody(args[0]), Void.class);
			log("uploaded");
		} else {
			log("not found key name");
		}
	}

	private HttpEntity<SshKeyPost> getBody(String name) {
		Path publicKeyPath = sshKeyLocalResolver.getPublicKeyPath(name);
		try {
			return new HttpEntity<>(new SshKeyPost(readString(publicKeyPath), name, false));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
