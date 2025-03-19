package art.aelaort;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
public class TestSpringContext {
	@Autowired
	private ApplicationContext applicationContext;
	@MockitoBean
	private Entrypoint entrypoint;

	@Test
	public void testContextLoads() {
		doNothing().when(entrypoint).run("");
		assertNotNull(applicationContext);
	}
}
