package art.aelaort;

import art.aelaort.models.PhysicalServer;
import art.aelaort.models.PhysicalServerLength;
import art.aelaort.models.Service;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StringFormattingService {
	public String serversServicesFullListString(List<PhysicalServer> servers) {
		StringBuilder sb = new StringBuilder();
		servers.forEach(server -> {
			String str = serverServicesString(server);
			if (!str.isEmpty()) {
				sb.append(str).append("\n");
			}
		});
		return sb.toString();
	}

	private String serverServicesString(PhysicalServer server) {
		if (server.getServices().isEmpty()) {
			return "";
		}

		return server.getName() + "\n" + servicesString(server.getServices());
	}

	private String servicesString(List<Service> services) {
		if (services.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, List<String>> servicesByYml : servicesListMap(services).entrySet()) {
			sb.append("\t")
					.append(servicesByYml.getKey())
					.append("\n\t\t");
			for (String serviceName : servicesByYml.getValue()) {
				sb.append(serviceName).append("\n\t\t");
			}
			sb.deleteCharAt(sb.length() - 1);
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	private Map<String, List<String>> servicesListMap(List<Service> services) {
		Map<String, List<String>> servicesMap = new HashMap<>();
		for (Service service : services) {
			String ymlName = service.getYmlName();
			if (servicesMap.containsKey(ymlName)) {
				servicesMap.get(ymlName).add(name(service));
			} else {
				servicesMap.put(ymlName, new ArrayList<>() {{
					add(name(service));
				}});
			}
		}
		return servicesMap;
	}

	/*
	 * ======================================================
	 * ======================================================
	 */

	public String physicalServersTableString(List<PhysicalServer> servers) {
		PhysicalServerLength lengths = getLengths(servers);
		String nameHeader = StringUtils.center("name", lengths.nameLength());
		String ipHeader = StringUtils.center("ip", lengths.ipLength());
		String monitoringHeader = StringUtils.center("monitoring", lengths.monitoringLength());
		String sshKeyHeader = StringUtils.center("sshKey", lengths.sshKeyLength());

		StringBuilder sb = new StringBuilder();
		sb.append(nameHeader).append(ipHeader).append(monitoringHeader).append(sshKeyHeader).append("\n");
		sb.append(StringUtils.repeat('-', lengths.sum())).append("\n");
		servers.forEach(server -> sb.append(toStr(server, lengths)).append("\n"));

		return sb.toString().replaceAll(" +$", "");
	}

	private String toStr(PhysicalServer obj, PhysicalServerLength lengths) {
		String nameStr = StringUtils.rightPad(obj.getName(), lengths.nameLength());
		String ipStr = StringUtils.rightPad(obj.getIp(), lengths.ipLength());
		String monitoringStr = StringUtils.rightPad(String.valueOf(obj.isMonitoring()), lengths.monitoringLength());
		String sshKeyStr = StringUtils.rightPad(obj.getSshKey(), lengths.sshKeyLength());

		String data = "%s %s %s %s".formatted(nameStr, ipStr, monitoringStr, sshKeyStr);

		return data + servicesShortString(obj.getServices());
	}

	private String servicesShortString(List<Service> services) {
		return String.join(", ", servicesJoinMap(services).values());
	}

	private Map<String, String> servicesJoinMap(List<Service> services) {
		Map<String, String> servicesMap = new HashMap<>();

		for (Service service : services) {
			String ymlName = service.getYmlName();
			if (servicesMap.containsKey(ymlName)) {
				String newValue = servicesMap.get(ymlName) + ", " + name(service);
				servicesMap.put(ymlName, newValue);
			} else {
				servicesMap.put(ymlName, name(service));
			}
		}
		return servicesMap;
	}

	/*
	 * ======================================================
	 * ======================================================
	 */

	private String name(Service obj) {
		return obj.getDockerName() != null ? obj.getDockerName() : obj.getService();
	}

	private PhysicalServerLength getLengths(List<PhysicalServer> servers) {
		PhysicalServerLength lengths = new PhysicalServerLength();
		lengths.monitoringLength(12);
		lengths.ipLength(16);
		for (PhysicalServer server : servers) {
			lengths.nameLength(Math.max(lengths.nameLength(), server.getName().length()));
			lengths.sshKeyLength(Math.max(lengths.sshKeyLength(), server.getSshKey().length()));
		}
		lengths.nameLength(lengths.nameLength() + 1);
		lengths.sshKeyLength(lengths.sshKeyLength() + 1);
		return lengths;
	}
}
