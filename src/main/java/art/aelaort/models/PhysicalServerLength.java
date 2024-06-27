package art.aelaort.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Accessors(fluent = true)
public class PhysicalServerLength {
	private int nameLength;
	private int ipLength;
	private int sshKeyLength;
	private int monitoringLength;

	public int sum() {
		return nameLength + ipLength + sshKeyLength + monitoringLength;
	}
}
