package art.aelaort;

import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VirtualBoxService {
	private final SystemProcess systemProcess;
	@Value("${virtualbox.name}")
	private String virtualboxName;

	public void up() {
		systemProcess.callProcessInheritIO("vboxmanage startvm %s --type headless".formatted(virtualboxName));
	}

	public void pause() {
		systemProcess.callProcessInheritIO("vboxmanage controlvm %s savestate".formatted(virtualboxName));
	}

	public void stop() {
		systemProcess.callProcessInheritIO("vboxmanage controlvm %s acpipowerbutton".formatted(virtualboxName));
	}
}
