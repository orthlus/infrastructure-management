package art.aelaort.data.parsers;

import art.aelaort.exceptions.K8sUnknownApiObjectException;
import art.aelaort.models.servers.K8sApp;
import art.aelaort.utils.Utils;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Yaml;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class K8sYamlParser {
	private final Utils utils;

	public List<K8sApp> parseK8sYmlFile(Path ymlFile) {
		List<KubernetesObject> k8sObjects = parse(ymlFile);
		List<K8sApp> result = new ArrayList<>(k8sObjects.size());

		for (KubernetesObject k8sObject : k8sObjects) {
			K8sApp obj;
			if (k8sObject instanceof V1Pod o) {
				obj = convert(o);
			} else if (k8sObject instanceof V1Deployment o) {
				obj = convert(o);
			} else if (k8sObject instanceof V1CronJob o) {
				obj = convert(o);
			} else {
				throw new RuntimeException();
			}
			result.add(clean(obj));
		}

		return result;
	}

	private K8sApp convert(V1CronJob cronJob) {
		return K8sApp.builder()
				.image(cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
				.name(cronJob.getMetadata().getName())
				.kind(cronJob.getKind())
				.schedule(cronJob.getSpec().getSchedule())
				.build();
	}

	private K8sApp convert(V1Deployment deployment) {
		return K8sApp.builder()
				.image(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage())
				.name(deployment.getMetadata().getName())
				.kind(deployment.getKind())
				.build();
	}

	private K8sApp convert(V1Pod pod) {
		return K8sApp.builder()
				.image(pod.getSpec().getContainers().get(0).getImage())
				.name(pod.getMetadata().getName())
				.kind(pod.getKind())
				.build();
	}

	private K8sApp clean(K8sApp k8sApp) {
		return k8sApp.withImage(utils.dockerImageClean(k8sApp.getImage()));
	}

	private List<KubernetesObject> parse(Path ymlFile) {
		try {
			List<Object> loadedList = Yaml.loadAll(ymlFile.toFile());
			List<KubernetesObject> result = new ArrayList<>(loadedList.size());
			for (Object loaded : loadedList) {
				if (!(loaded instanceof KubernetesObject)) {
					throw new K8sUnknownApiObjectException();
				}
				result.add((KubernetesObject) loaded);
			}
			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
