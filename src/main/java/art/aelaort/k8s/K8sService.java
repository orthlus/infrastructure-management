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
			log("not enough args: registry, login, password. Also you can pass docker login command");
			System.exit(1);
		}

		Cred cred;
		if (args[0].equals("docker") && args[1].equals("login")) {
			cred = new Cred(args[6], args[3], args[5]);
		} else {
			cred = new Cred(args[0], args[1], args[2]);
		}
		String auth = Base64.getEncoder().encodeToString((cred.login() + ":" + cred.password()).getBytes());
		String json = """
				{"auths":{"%s":{"username":"%s","password":"%s","auth":"%s"}}}"""
				.formatted(cred.registry(), cred.login(), cred.password(), auth);
		String encoded = Base64.getEncoder().encodeToString(json.getBytes());
		log(encoded);
	}

	private record Cred(String registry, String login, String password) {
	}
}
