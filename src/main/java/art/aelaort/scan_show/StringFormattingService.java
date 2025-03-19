package art.aelaort.scan_show;

import art.aelaort.models.build.Job;
import art.aelaort.models.servers.Server;
import art.aelaort.models.servers.ServiceDto;
import dnl.utils.text.table.TextTable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static art.aelaort.utils.TablePrintingUtils.*;

@Component
public class StringFormattingService {
	public String servicesByServerString(List<Server> servers, Map<String, Job> jobs) {
		String[] columnNames = {"server", "image", "type", "app", "file"};
		Object[][] data = convertServicesToArrays(mapToAppRows(servers, jobs));
		TextTable tt = new TextTable(columnNames, data);
		tt.setAddRowNumbering(true);
		return "services:\n" + getTableString(tt);
	}

	private List<AppRow> mapToAppRows(List<Server> servers, Map<String, Job> jobs) {
		List<AppRow> res = new ArrayList<>();
		for (Server server : servers) {
			for (ServiceDto service : server.getServices()) {
				String image = service.getDockerImageName();
				Job job = image != null ? jobs.get(image.split(":")[0]) : null;
				String type = job != null ? job.getBuildType().toString() : null;
				String appName = getAppName(service);
				AppRow appRow = new AppRow(server.getName(), image, type, appName, service.getYmlName());
				res.add(appRow);
			}
		}
		return res;
	}

	private Object[][] convertServicesToArrays(List<AppRow> appRows) {
		Object[][] result = new Object[appRows.size()][5];
		for (int i = 0; i < appRows.size(); i++) {
			AppRow appRow = appRows.get(i);
			result[i][0] = appRow.server();
			result[i][1] = nullable(appRow.image());
			result[i][2] = nullable(appRow.type());
			result[i][3] = appRow.app();
			result[i][4] = appRow.file();
		}

		appendSpaceToRight(result);

		return result;
	}

	private String getAppName(ServiceDto service) {
		return service.getDockerName() == null ?
				service.getService() :
				service.getDockerName() + " - " + service.getService();
	}

	record AppRow(String server, String image, String type, String app, String file) {}

	/*
	 * ======================================================
	 * ======================================================
	 */

	public String getServersTableString(List<Server> servers) {
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
		return "servers:\n" + getTableString(tt);
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
}
