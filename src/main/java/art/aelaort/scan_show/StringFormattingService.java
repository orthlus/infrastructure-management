package art.aelaort.scan_show;

import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.ServerDataLength;
import art.aelaort.models.servers.ServiceDto;
import dnl.utils.text.table.TextTable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static art.aelaort.utils.TablePrintingUtils.*;
import static art.aelaort.utils.Utils.log;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.*;

@Component
public class StringFormattingService {
	public String servicesByServerString(List<Server> servers) {
		log("services:");
		String[] columnNames = {"server", "app", "file"};
		Object[][] data = convertServicesToArrays(mapToAppRows(servers));
		TextTable tt = new TextTable(columnNames, data);
		tt.setAddRowNumbering(true);
		return getTableString(tt);
	}

	private List<AppRow> mapToAppRows(List<Server> servers) {
		List<AppRow> res = new ArrayList<>();
		for (Server server : servers) {
			for (ServiceDto service : server.getServices()) {
				AppRow appRow = new AppRow(server.getName(), getAppName(service), service.getYmlName());
				res.add(appRow);
			}
		}
		return res;
	}

	private Object[][] convertServicesToArrays(List<AppRow> appRows) {
		Object[][] result = new Object[appRows.size()][3];
		for (int i = 0; i < appRows.size(); i++) {
			AppRow appRow = appRows.get(i);
			result[i][0] = appRow.server();
			result[i][1] = appRow.app();
			result[i][2] = appRow.file();
		}

		appendSpaceToRight(result);

		return result;
	}

	private String getAppName(ServiceDto service) {
		return service.getDockerName() == null ?
				service.getService() :
				service.getDockerName() + " - " + service.getService();
	}

	record AppRow(String server, String app, String file) {}

	/*
	 * ======================================================
	 * ======================================================
	 */

	public String getServersTableString(List<Server> servers) {
		log("servers:");
		String[] columnNames = {
				"id",
				"name",
				"ip",
				"port",
				"monitoring",
				"price",
				"sshKey",
				"services"
		};

		Object[][] data = convertServersToArrays(servers);
		TextTable tt = new TextTable(columnNames, data);
		return getTableString(tt);
	}

	private Object[][] convertServersToArrays(List<Server> servers) {
		Object[][] result = new Object[servers.size()][8];
		for (int i = 0; i < servers.size(); i++) {
			Server server = servers.get(i);
			result[i][0] = server.getId();
			result[i][1] = server.getName();
			result[i][2] = nullable(server.getIp());
			result[i][3] = nullable(server.getPort());
			result[i][4] = server.isMonitoring();
			result[i][5] = nullable(server.getPrice());
			result[i][6] = nullable(server.getSshKey());
			result[i][7] = Server.servicesStr(server.getServices());
		}

		appendSpaceToRight(result);

		return result;
	}

	@Deprecated
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

	@Deprecated
	private String toStr(Server obj, ServerDataLength lengths) {
		String nameStr = rightPad(obj.getName(), lengths.nameLength());
		String ipStr = rightPad(obj.getIp(), lengths.ipLength());
		String portStr = rightPad(valueOf(obj.getPort()), lengths.portLength());
		String monitoringStr = rightPad(valueOf(obj.isMonitoring()), lengths.monitoringLength());
		String sshKeyStr = rightPad(obj.getSshKey(), lengths.sshKeyLength());
		String servicesStr = rightPad(Server.servicesStr(obj.getServices()), lengths.servicesLength());

		return "%s %s %s %s %s %s".formatted(nameStr, ipStr, portStr, monitoringStr, sshKeyStr, servicesStr);
	}

	@Deprecated
	private ServerDataLength getLengths(List<Server> servers) {
		ServerDataLength lengths = new ServerDataLength();
		lengths.monitoringLength(12);
		lengths.ipLength(16);
		lengths.portLength(6);
		for (Server server : servers) {
			lengths.nameLength(Math.max(lengths.nameLength(), server.getName().length()));
			lengths.sshKeyLength(Math.max(lengths.sshKeyLength(), server.getSshKey().length()));
			lengths.servicesLength(Math.max(lengths.servicesLength(), Server.servicesStr(server.getServices()).length()));
		}
		lengths.nameLength(lengths.nameLength() + 1);
		lengths.ipLength(lengths.ipLength() + 1);
		lengths.sshKeyLength(lengths.sshKeyLength() + 1);
		return lengths;
	}
}
