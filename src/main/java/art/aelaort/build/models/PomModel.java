package art.aelaort.build.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "project")
public class PomModel {
	@JacksonXmlProperty
	Properties properties;

	public Integer getMavenCompilerTarget() {
		try {
			return properties.mavenCompilerTarget;
		} catch (Exception e) {
			return null;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Properties {
		@JacksonXmlProperty(localName = "maven.compiler.target")
		Integer mavenCompilerTarget;
	}
}
