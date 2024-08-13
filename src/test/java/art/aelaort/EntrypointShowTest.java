package art.aelaort;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(args = {"show"})
class EntrypointShowTest {
	@Autowired
	private Entrypoint entrypoint;

	@Test
	public void run() {
	}
}
