package art.aelaort.exceptions;

import art.aelaort.models.make.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AppNotFoundException extends RuntimeException {
	private Project project;
}
