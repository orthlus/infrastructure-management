package art.aelaort.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PhysicalServer {
	private String name;
	private String ip;
	private String sshKey;
	private boolean monitoring;
	private List<Service> services;
}
