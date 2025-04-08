package art.aelaort.k8s;

import art.aelaort.models.servers.K8sApp;
import art.aelaort.models.servers.K8sCluster;
import art.aelaort.models.servers.K8sService;
import art.aelaort.models.servers.display.ClusterAppRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

public class K8sUtils {
	public static List<ClusterAppRow> mapToClusterAppRows(List<K8sCluster> clusters) {
		List<ClusterAppRow> res = new ArrayList<>();
		for (K8sCluster cluster : clusters) {
			Map<String, K8sService> serviceByPodNameMap = makeServiceByPodNameMap(cluster);
			for (K8sApp app : cluster.apps()) {
				K8sService service = serviceByPodNameMap.get(app.getPodName());
				ClusterAppRow clusterAppRow = new ClusterAppRow(cluster.name(),
						app.getNamespace(),
						app.getImage(),
						app.getName(),
						renameKind(app.getKind()),
						app.getImagePullPolicy(),
						servicePortsString(service),
						serviceType(service),
						app.getSchedule(),
						app.getMemoryLimit(),
						app.getStrategyType(),
						serviceRoute(service)
				);
				res.add(clusterAppRow);
			}
		}
		return res;
	}

	private static String renameKind(String kind) {
		return switch (kind) {
			case "Deployment": yield "D";
			case "DaemonSet": yield "DS";
			case "CronJob": yield "CJ";
			default: yield kind;
		};
	}

	private static Map<String, K8sService> makeServiceByPodNameMap(K8sCluster cluster) {
		Map<String, K8sService> res = new HashMap<>();
		for (K8sApp app : cluster.apps()) {
			if (hasText(app.getPodName())) {
				for (K8sService service : cluster.services()) {
					if (service.getAppSelector().equals(app.getPodName()) && service.getNamespace().equals(app.getNamespace())) {
						res.put(app.getPodName(), service);
					}
				}
			}
		}
		return res;
	}

	private static String serviceRoute(K8sService service) {
		if (service == null) {
			return null;
		}
		return service.getRoute();
	}

	private static String serviceType(K8sService service) {
		if (service == null) {
			return null;
		}
		return service.getType();
	}

	private static String enrichPorts(String port, Boolean hasAnotherPorts) {
		if (hasAnotherPorts != null && hasAnotherPorts) {
			return port + " (+)";
		}
		return port;
	}

	private static String servicePortsString(K8sService service) {
		if (service == null || service.getPort() == null) {
			return null;
		}

		String result;
		if (service.getNodePort() == null) {
			if (hasText(service.getTargetPort())) {
				result = "%s %s".formatted(service.getPort(), service.getTargetPort());
			} else {
				result = "%s %s".formatted(service.getPort(), service.getPort());
			}
		} else {
			if (hasText(service.getTargetPort())) {
				result = "%s %s %s".formatted(service.getNodePort(), service.getPort(), service.getTargetPort());
			} else {
				result = "%s %s %s".formatted(service.getNodePort(), service.getPort(), service.getPort());
			}
		}
		return enrichPorts(result, service.getHasAnotherPorts());
	}
}
