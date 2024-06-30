package art.aelaort;

import art.aelaort.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExternalUtilities {
	private final SystemProcess systemProcess;

	public void ydSync() {
		systemProcess.callProcess("wsl yandex-disk sync");
	}
}
