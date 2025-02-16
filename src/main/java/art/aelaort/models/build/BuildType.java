package art.aelaort.models.build;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum BuildType {
	java_docker("jd"),
	docker("d"),
	java_local("jl"),
	java_graal_local("jg"),
	ya_func("yf"),
	frontend_vue("fv");
	public final String alias;
}
