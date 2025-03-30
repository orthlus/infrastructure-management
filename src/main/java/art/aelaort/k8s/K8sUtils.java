package art.aelaort.k8s;

import art.aelaort.models.servers.K8sApp;
import art.aelaort.models.servers.K8sCluster;
import art.aelaort.models.servers.K8sService;
import art.aelaort.models.servers.display.ClusterAppRow;
import io.fabric8.kubernetes.api.model.IntOrString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.util.StringUtils.hasText;

public class K8sUtils {
	public static String unwrap(IntOrString intOrString) {
		if (intOrString == null) {
			return null;
		}

		if (intOrString.getStrVal() != null) {
			return intOrString.getStrVal();
		} else {
			return String.valueOf(intOrString.getIntVal());
		}
	}

	public static List<ClusterAppRow> mapToClusterAppRows(List<K8sCluster> clusters) {
		List<ClusterAppRow> res = new ArrayList<>();
		for (K8sCluster cluster : clusters) {
			Map<String, K8sService> serviceByPodNameMap = makeServiceByPodNameMap(cluster);
			for (K8sApp app : cluster.apps()) {
				ClusterAppRow clusterAppRow = new ClusterAppRow(cluster.name(),
						app.getImage(),
						app.getName(),
						app.getKind(),
						servicePortsString(serviceByPodNameMap.get(app.getPodName())),
						serviceType(serviceByPodNameMap.get(app.getPodName())),
						app.getSchedule(),
						app.getStrategyType(),
						serviceAnotherPorts(serviceByPodNameMap.get(app.getPodName())));
				res.add(clusterAppRow);
			}
		}
		return res;
	}

	private static Map<String, K8sService> makeServiceByPodNameMap(K8sCluster cluster) {
		Map<String, K8sService> res = new HashMap<>();
		for (K8sApp app : cluster.apps()) {
			if (hasText(app.getPodName())) {
				for (K8sService service : cluster.services()) {
					if (service.getAppSelector().equals(app.getPodName())) {
						res.put(app.getPodName(), service);
					}
				}
			}
		}
		return res;
	}

	private static String serviceAnotherPorts(K8sService service) {
		if (service == null) {
			return null;
		}
		return String.valueOf(service.getHasAnotherPorts());
	}

	private static String serviceType(K8sService service) {
		if (service == null) {
			return null;
		}
		return service.getType();
	}

	private static String servicePortsString(K8sService service) {
		if (service == null || service.getPort() == null) {
			return null;
		}

		if (service.getNodePort() == null) {
			if (hasText(service.getTargetPort())) {
				return "%s:%s".formatted(service.getPort(), service.getTargetPort());
			} else {
				return "%s:%s".formatted(service.getPort(), service.getPort());
			}
		} else {
			if (hasText(service.getTargetPort())) {
				return "%s:%s:%s".formatted(service.getNodePort(), service.getPort(), service.getTargetPort());
			} else {
				return "%s:%s:%s".formatted(service.getNodePort(), service.getPort(), service.getPort());
			}
		}
	}
}
