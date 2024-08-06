package art.aelaort;

import art.aelaort.models.Server;
import art.aelaort.models.ServerDataLength;
import art.aelaort.models.ServiceDto;
import dnl.utils.text.table.TextTable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.join;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.*;

@Component
public class StringFormattingService {
	public String servicesByServerFullTreeString(List<Server> servers) {
		StringBuilder sb = new StringBuilder("services:\n");
		for (Server server : servers) {
			if (server.getServices().isEmpty()) {
				continue;
			}
			sb.append(server.getName())
					.append("\n")
					.append(servicesString(server.getServices()));
		}
		return sb.toString().trim();
	}

	private String servicesString(List<ServiceDto> services) {
		if (services.isEmpty()) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, List<String>> servicesByYml : servicesMapList(services).entrySet()) {
			String join = join("\n      ", servicesByYml.getValue());
			sb.append("   ")
					.append(servicesByYml.getKey())
					.append("\n      ")
					.append(join);
		}
		return sb.append("\n").toString();
	}

	private Map<String, List<String>> servicesMapList(List<ServiceDto> services) {
		Map<String, List<String>> servicesMap = new HashMap<>();
		for (ServiceDto service : services) {
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

	public void printServersTableString(List<Server> servers) {
		System.out.println("servers:");
		String[] columnNames = {
				"name",
				"ip",
				"port",
				"monitoring",
				"sshKey",
				"services"
		};

		Object[][] data = convertServersToArrays(servers);
		TextTable tt = new TextTable(columnNames, data);
		tt.setAddRowNumbering(true);
		tt.printTable();
	}

	private Object[][] convertServersToArrays(List<Server> servers) {
		Object[][] result = new Object[servers.size()][6];
		for (int i = 0; i < servers.size(); i++) {
			Server server = servers.get(i);
			result[i][0] = server.getName();
			result[i][1] = server.getIp();
			result[i][2] = server.getPort() == -1 ? "null" : server.getPort();
			result[i][3] = server.isMonitoring();
			result[i][4] = server.getSshKey();
			result[i][5] = server.getServicesStr();
		}

		return result;
	}

	public String serversTableString(List<Server> servers) {
		ServerDataLength lengths = getLengths(servers);
		String nameHeader = center("name", lengths.nameLength());
		String ipHeader = center("ip", lengths.ipLength());
		String portHeader = center("port", lengths.portLength());
		String monitoringHeader = center("monitoring", lengths.monitoringLength());
		String sshKeyHeader = center("sshKey", lengths.sshKeyLength());
		String servicesHeader = center("services", lengths.servicesLength());

		StringBuilder sb = new StringBuilder("servers:\n");
		sb.append(repeat('-', lengths.sum())).append("\n");
		sb.append(nameHeader)
				.append(ipHeader)
				.append(portHeader)
				.append(monitoringHeader)
				.append(sshKeyHeader)
				.append(servicesHeader)
				.append("\n");
		sb.append(repeat('-', lengths.sum())).append("\n");
		servers.forEach(server -> sb.append(toStr(server, lengths)).append("\n"));

		return sb.toString().replaceAll(" +$", "");
	}

	private String toStr(Server obj, ServerDataLength lengths) {
		String nameStr = rightPad(obj.getName(), lengths.nameLength());
		String ipStr = rightPad(obj.getIp(), lengths.ipLength());
		String portStr = rightPad(valueOf(obj.getPort()), lengths.portLength());
		String monitoringStr = rightPad(valueOf(obj.isMonitoring()), lengths.monitoringLength());
		String sshKeyStr = rightPad(obj.getSshKey(), lengths.sshKeyLength());
		String servicesStr = rightPad(obj.getServicesStr(), lengths.servicesLength());

		return "%s %s %s %s %s %s".formatted(nameStr, ipStr, portStr, monitoringStr, sshKeyStr, servicesStr);
	}

	private ServerDataLength getLengths(List<Server> servers) {
		ServerDataLength lengths = new ServerDataLength();
		lengths.monitoringLength(12);
		lengths.ipLength(16);
		lengths.portLength(6);
		for (Server server : servers) {
			lengths.nameLength(Math.max(lengths.nameLength(), server.getName().length()));
			lengths.sshKeyLength(Math.max(lengths.sshKeyLength(), server.getSshKey().length()));
			lengths.servicesLength(Math.max(lengths.servicesLength(), server.getServicesStr().length()));
		}
		lengths.nameLength(lengths.nameLength() + 1);
		lengths.ipLength(lengths.ipLength() + 1);
		lengths.sshKeyLength(lengths.sshKeyLength() + 1);
		return lengths;
	}
}
