package art.aelaort.exceptions;

import art.aelaort.system.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class DockerComposeValidationFailedException extends RuntimeException {
	private final Response response;
}
