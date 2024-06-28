package art.aelaort;

import art.aelaort.models.PhysicalServer;
import art.aelaort.models.PhysicalServerLength;
import art.aelaort.models.Service;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.*;

@Component
public class StringFormattingService {
	public String servicesByServerFullTreeString(List<PhysicalServer> servers) {
		StringBuilder sb = new StringBuilder();
		for (PhysicalServer server : servers) {
			if (server.getServices().isEmpty()) {
				continue;
			}
			sb.append(server.getName())
					.append("\n")
					.append(servicesString(server.getServices()))
					.append("\n");
		}
		return sb.toString();
	}

	private String servicesString(List<Service> services) {
		if (services.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, List<String>> servicesByYml : servicesMapList(services).entrySet()) {
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

	private Map<String, List<String>> servicesMapList(List<Service> services) {
		Map<String, List<String>> servicesMap = new HashMap<>();
		for (Service service : services) {
			String ymlName = service.getYmlName();
			String name = service.getDockerName() == null ?
					service.getService() :
					service.getDockerName() + " - " + service.getService();
			servicesMap
					.computeIfAbsent(ymlName, k -> new ArrayList<>())
					.add(name);
		}
		return servicesMap;
	}

	/*
	 * ======================================================
	 * ======================================================
	 */

	public String serversTableString(List<PhysicalServer> servers) {
		PhysicalServerLength lengths = getLengths(servers);
		String nameHeader = center("name", lengths.nameLength());
		String ipHeader = center("ip", lengths.ipLength());
		String monitoringHeader = center("monitoring", lengths.monitoringLength());
		String sshKeyHeader = center("sshKey", lengths.sshKeyLength());

		StringBuilder sb = new StringBuilder();
		sb.append(nameHeader).append(ipHeader).append(monitoringHeader).append(sshKeyHeader).append("\n");
		sb.append(repeat('-', lengths.sum())).append("\n");
		servers.forEach(server -> sb.append(toStr(server, lengths)).append("\n"));

		return sb.toString().replaceAll(" +$", "");
	}

	private String toStr(PhysicalServer obj, PhysicalServerLength lengths) {
		String nameStr = rightPad(obj.getName(), lengths.nameLength());
		String ipStr = rightPad(obj.getIp(), lengths.ipLength());
		String monitoringStr = rightPad(String.valueOf(obj.isMonitoring()), lengths.monitoringLength());
		String sshKeyStr = rightPad(obj.getSshKey(), lengths.sshKeyLength());

		String data = "%s %s %s %s".formatted(nameStr, ipStr, monitoringStr, sshKeyStr);

		return data + join(", ", servicesMapJoin(obj.getServices()).values());
	}

	private Map<String, String> servicesMapJoin(List<Service> services) {
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
