package art.aelaort.k8s;

import art.aelaort.models.servers.K8sApp;
import art.aelaort.models.servers.K8sService;
import art.aelaort.utils.Utils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentStrategy;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static art.aelaort.k8s.K8sUtils.unwrap;
import static art.aelaort.utils.ColoredConsoleTextUtils.wrapRed;
import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class K8sYamlParser {
	private final Utils utils;

	public List<K8sService> parseK8sYmlFileForServices(Path ymlFile) {
		List<HasMetadata> k8sObjects = parse(ymlFile);
		List<K8sService> result = new ArrayList<>(k8sObjects.size());

		for (HasMetadata k8sObject : k8sObjects) {
			K8sService obj;
			if (k8sObject instanceof Service o) {
				obj = convert(o);
			} else {
				continue;
			}
			result.add(obj);
		}

		return result;
	}

	private K8sService convert(Service service) {
		K8sService.K8sServiceBuilder builder = K8sService.builder()
				.name(service.getMetadata().getName())
				.kind(service.getKind())
				.type(service.getSpec().getType())
				.appSelector(service.getSpec().getSelector().get("app"));
		enrichWithPorts(builder, service);
		return builder
				.build();
	}

	private void enrichWithPorts(K8sService.K8sServiceBuilder builder, Service service) {
		List<ServicePort> ports = service.getSpec().getPorts();
		if (ports == null || ports.isEmpty()) {
			return;
		}

		if (ports.size() > 1) {
			log(wrapRed("several ports find in service %s, not supported".formatted(service.getMetadata().getName())));
		}

		ServicePort port = ports.get(0);

		builder.port(port.getPort());

		if (port.getTargetPort() != null && port.getTargetPort().getValue() != null) {
			builder.targetPort(unwrap(port.getTargetPort()));
		}

		if (port.getNodePort() != null) {
			builder.nodePort(port.getNodePort());
		}
	}

	public List<K8sApp> parseK8sYmlFileForApps(Path ymlFile) {
		List<HasMetadata> k8sObjects = parse(ymlFile);
		List<K8sApp> result = new ArrayList<>(k8sObjects.size());

		for (HasMetadata k8sObject : k8sObjects) {
			K8sApp obj;
			if (k8sObject instanceof Pod o) {
				obj = convert(o);
			} else if (k8sObject instanceof Deployment o) {
				obj = convert(o);
			} else if (k8sObject instanceof CronJob o) {
				obj = convert(o);
			} else {
				continue;
			}
			result.add(clean(obj));
		}

		return result;
	}

	private K8sApp convert(CronJob cronJob) {
		return K8sApp.builder()
				.image(cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
				.name(cronJob.getMetadata().getName())
				.kind(cronJob.getKind())
				.schedule(cronJob.getSpec().getSchedule())
				.build();
	}

	private K8sApp convert(Deployment deployment) {
		DeploymentStrategy strategy = deployment.getSpec().getStrategy();
		return K8sApp.builder()
				.image(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
				.podName(deployment.getSpec().getTemplate().getMetadata().getLabels().get("app"))
				.name(deployment.getMetadata().getName())
				.kind(deployment.getKind())
				.strategyType(strategy == null ? null : strategy.getType())
				.build();
	}

	private K8sApp convert(Pod pod) {
		return K8sApp.builder()
				.image(pod.getSpec().getContainers().get(0).getImage())
				.name(pod.getMetadata().getName())
				.podName(pod.getMetadata().getName())
				.kind(pod.getKind())
				.build();
	}

	private K8sApp clean(K8sApp k8sApp) {
		if (k8sApp.getImage() == null) {
			return k8sApp;
		}
		return k8sApp.withImage(utils.dockerImageClean(k8sApp.getImage()));
	}

	private List<HasMetadata> parse(Path ymlFile) {
		try (KubernetesClient client = new KubernetesClientBuilder().build()) {
			return client.load(Files.newInputStream(ymlFile)).items();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
