package art.aelaort;

import art.aelaort.utils.system.SystemProcess;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class VirtualBoxService {
	private final SystemProcess systemProcess;
	@Value("${virtualbox.names}")
	private List<String> virtualboxNames;

	public void up(String... args) {
		systemProcess.callProcessInheritIO("vboxmanage startvm %s --type headless".formatted(nameById(args)));
	}

	public void pause(String... args) {
		systemProcess.callProcessInheritIO("vboxmanage controlvm %s savestate".formatted(nameById(args)));
	}

	public void stop(String... args) {
		systemProcess.callProcessInheritIO("vboxmanage controlvm %s acpipowerbutton".formatted(nameById(args)));
	}

	private String nameById(String... args) {
		if (args.length > 0) {
			try {
				return virtualboxNames.get(Integer.parseInt(args[0]));
			} catch (NumberFormatException | IndexOutOfBoundsException e) {
				throw new RuntimeException(e);
			}
		} else {
			log("at least one arg required");
			throw new RuntimeException();
		}
	}

	public String list() {
		return String.join("\n", virtualboxNames);
	}
}
