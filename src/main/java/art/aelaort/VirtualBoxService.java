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
		checkArgs(args);
		systemProcess.callProcessInheritIO("vboxmanage startvm %s --type headless".formatted(nameById(args)));
	}

	public void pause(String... args) {
		checkArgs(args);
		systemProcess.callProcessInheritIO("vboxmanage controlvm %s savestate".formatted(nameById(args)));
	}

	public void stop(String... args) {
		checkArgs(args);
		systemProcess.callProcessInheritIO("vboxmanage controlvm %s acpipowerbutton".formatted(nameById(args)));
	}

	private void checkArgs(String... args) {
		if (args.length <= 1) {
			log("at least 2 args required. list vms:");
			log(list());
		}
	}

	private String nameById(String... args) {
		if (args.length > 1) {
			try {
				return virtualboxNames.get(Integer.parseInt(args[1]) - 1);
			} catch (NumberFormatException | IndexOutOfBoundsException e) {
				log("wrong number or not a number");
				throw new RuntimeException(e);
			}
		} else {
			log("at least 2 args required");
			throw new RuntimeException();
		}
	}

	public String list() {
		return String.join("\n", virtualboxNames);
	}
}
