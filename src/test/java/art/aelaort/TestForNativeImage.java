package art.aelaort;

import art.aelaort.k8s.K8sClusterProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
public class TestForNativeImage {
	@MockitoBean
	private Entrypoint entrypoint;
	@Autowired
	private K8sClusterProvider k8sClusterProvider;

	@BeforeEach
	public void initAll() {
		doNothing().when(entrypoint).run("");
	}

	@Test
	public void testAllForReflection() {
		assertThat(k8sClusterProvider.getClusters()).isNotEmpty();
		assertThat(k8sClusterProvider.getClustersFromLocalConfig()).isNotEmpty();
	}
}
