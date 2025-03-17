package art.aelaort.k8s;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class K8sService {
	private final K8sProps k8sProps;

	public void apply(String[] args) {

	}

	public void printDockerConfigJson(String[] args) {
		if (args.length < 3) {
			log("not enough args: registry, login, password");
			System.exit(1);
		}

		String registry = args[0];
		String login = args[1];
		String password = args[2];
		String auth = Base64.getEncoder().encodeToString((login + ":" + password).getBytes());
		String json = """
						{"auths":{"%s":{"username":"%s","password":"%s","auth":"%s"}}}"""
				.formatted(registry, login, password, auth);
		String encoded = Base64.getEncoder().encodeToString(json.getBytes());
		log(encoded);
	}
}
