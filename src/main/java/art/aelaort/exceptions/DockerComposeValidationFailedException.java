package art.aelaort.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class DockerComposeValidationFailedException extends RuntimeException {
	private final String stderr;
}
