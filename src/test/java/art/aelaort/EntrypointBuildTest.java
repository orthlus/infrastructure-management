package art.aelaort;

import art.aelaort.models.build.Job;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(args = {"build"})
class EntrypointBuildTest {
	@Autowired
	private BuildService buildService;

	@Test
	public void run() {
		Job job = buildService.getJobById(1);
	}
}
