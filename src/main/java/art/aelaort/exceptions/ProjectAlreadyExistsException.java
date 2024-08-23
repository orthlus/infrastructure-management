package art.aelaort.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

@AllArgsConstructor
@Getter
public class ProjectAlreadyExistsException extends RuntimeException {
	private Path dir;
}
