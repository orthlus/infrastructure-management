package art.aelaort.scan_show;

import art.aelaort.models.build.Job;
import art.aelaort.models.servers.K8sApp;
import art.aelaort.models.servers.K8sCluster;
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
	public String servicesByServerString(List<Server> servers, Map<String, Job> jobs, List<K8sCluster> clusters) {
		String[] columnNames = {"server", "k8sCluster", "image", "type", "app", "file"};
		Object[][] data = convertServicesToArrays(mapToAppRows(servers, jobs, clusters), columnNames);
		TextTable tt = new TextTable(columnNames, data);
		tt.setAddRowNumbering(true);
		return "services:\n" + getTableString(tt);
	}

	private List<AppRow> mapToAppRows(List<Server> servers, Map<String, Job> jobs, List<K8sCluster> clusters) {
		List<AppRow> res = new ArrayList<>();
		for (Server server : servers) {
			for (ServiceDto service : server.getServices()) {
				String image = service.getDockerImageName();
				Job job = image != null ? jobs.get(image.split(":")[0]) : null;
				String type = job != null ? job.getBuildType().toString() : null;
				String appName = getAppName(service);
				AppRow appRow = new AppRow(server.getName(), null, image, type, appName, service.getYmlName());
				res.add(appRow);
			}
		}
		for (K8sCluster cluster : clusters) {
			for (K8sApp app : cluster.apps()) {
				AppRow appRow = new AppRow(null, cluster.name(), app.getImage(), null, app.getName(), null);
				res.add(appRow);
			}
		}
		return res;
	}

	private Object[][] convertServicesToArrays(List<AppRow> appRows, String[] columnNames) {
		Object[][] result = new Object[appRows.size()][columnNames.length];
		for (int i = 0; i < appRows.size(); i++) {
			AppRow appRow = appRows.get(i);
			result[i][0] = nullable(appRow.server());
			result[i][1] = nullable(appRow.k8sCluster());
			result[i][2] = nullable(appRow.image());
			result[i][3] = nullable(appRow.type());
			result[i][4] = nullable(appRow.app());
			result[i][5] = nullable(appRow.file());
		}

		appendSpaceToRight(result);

		return result;
	}

	private String getAppName(ServiceDto service) {
		return service.getDockerName() == null ?
				service.getService() :
				service.getDockerName() + " - " + service.getService();
	}

	record AppRow(String server, String k8sCluster, String image, String type, String app, String file) {}

	/*
	 * ======================================================
	 * ======================================================
	 */

	public String getK8sTableString(List<K8sCluster> clusters) {
		String[] columnNames = {
				"cluster",
				"image",
				"name",
				"kind",
				"schedule",
		};

		Object[][] data = convertClustersToArrays(mapToClusterAppRows(clusters), columnNames);
		TextTable tt = new TextTable(columnNames, data);
		return "k8s clusters and apps:\n" + getTableString(tt);
	}

	private List<ClusterAppRow> mapToClusterAppRows(List<K8sCluster> clusters) {
		List<ClusterAppRow> res = new ArrayList<>();
		for (K8sCluster cluster : clusters) {
			for (K8sApp app : cluster.apps()) {
				ClusterAppRow clusterAppRow = new ClusterAppRow(cluster.name(), app.getImage(), app.getName(), app.getKind(), app.getSchedule());
				res.add(clusterAppRow);
			}
		}
		return res;
	}

	private Object[][] convertClustersToArrays(List<ClusterAppRow> clusters, String[] columnNames) {
		Object[][] result = new Object[clusters.size()][columnNames.length];
		for (int i = 0; i < clusters.size(); i++) {
			ClusterAppRow app = clusters.get(i);
			result[i][0] = app.cluster();
			result[i][1] = nullable(app.image());
			result[i][2] = nullable(app.name());
			result[i][3] = app.kind();
			result[i][4] = nullable(app.schedule());
		}

		appendSpaceToRight(result);

		return result;
	}

	record ClusterAppRow(String cluster, String image, String name, String kind, String schedule) {}

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

		Object[][] data = convertServersToArrays(servers, columnNames);
		TextTable tt = new TextTable(columnNames, data);
		return "servers:\n" + getTableString(tt);
	}

	private Object[][] convertServersToArrays(List<Server> servers, String[] columnNames) {
		Object[][] result = new Object[servers.size()][columnNames.length];
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
