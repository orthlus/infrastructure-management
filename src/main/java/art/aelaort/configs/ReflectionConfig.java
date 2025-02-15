package art.aelaort.configs;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration
@RegisterReflectionForBinding({
		art.aelaort.models.build.Job.class,
		art.aelaort.models.build.PomModel.class,
		art.aelaort.models.build.PomModel.Properties.class,
		art.aelaort.models.servers.Server.class,
		art.aelaort.models.servers.yaml.TabbyFile.class,
		art.aelaort.models.servers.yaml.CustomFile.class,
		art.aelaort.models.servers.yaml.DockerComposeFile.class,
		com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.class,
		art.aelaort.ssh.tw.SshKeyPost.class,
})
public class ReflectionConfig {
}
