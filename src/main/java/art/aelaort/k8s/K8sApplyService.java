package art.aelaort.k8s;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class K8sApplyService {
	private final K8sProps k8sProps;

	public void apply(String[] args) {

	}
}
