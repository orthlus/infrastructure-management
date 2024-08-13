package art.aelaort;

import art.aelaort.models.build.Job;
import art.aelaort.models.servers.Server;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import static org.springframework.util.ReflectionUtils.accessibleConstructor;
import static org.springframework.util.ReflectionUtils.findMethod;

//@Configuration
//@ImportRuntimeHints(JacksonRuntimeHints.PropertyNamingStrategyRegistrar.class)
public class JacksonRuntimeHints {
	static class PropertyNamingStrategyRegistrar implements RuntimeHintsRegistrar {
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			/*try {
				hints
						.reflection()
						.registerField(PropertyNamingStrategies.class.getDeclaredField("SNAKE_CASE"));
			} catch (NoSuchFieldException e) {
				// ...
			}*/


			try {
				// Register method for reflection
//				hints.reflection().registerMethod(findMethod(MyClass.class, "sayHello", String.class), ExecutableMode.INVOKE);

				hints.reflection().registerConstructor(accessibleConstructor(Server.class, String.class), ExecutableMode.INVOKE);
				hints.reflection().registerConstructor(accessibleConstructor(Job.class, String.class), ExecutableMode.INVOKE);

				// Register serialization
				hints.serialization().registerType(TypeReference.of(Server.class));
				hints.serialization().registerType(TypeReference.of(Job.class));

			// Register resources
			hints.resources().registerPattern("my-resource.txt");
			// Register proxy
//			hints.proxies().registerJdkProxy(MyInterface.class);
			} catch (Exception ignored) {

			}
		}
	}

}