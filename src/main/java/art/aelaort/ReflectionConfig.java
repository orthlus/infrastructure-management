package art.aelaort;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration
@RegisterReflectionForBinding({
		art.aelaort.models.build.Job.class,
		art.aelaort.models.servers.Server.class,
		art.aelaort.models.servers.yaml.TabbyFile.class,
		art.aelaort.models.servers.yaml.CustomFile.class,
		art.aelaort.models.servers.yaml.DockerComposeFile.class,
})
public class ReflectionConfig {
}
