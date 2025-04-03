package art.aelaort.configs;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Configuration
public class Config {
	@Bean
	public YAMLMapper yamlMapper() {
		return new YAMLMapper();
	}

	@Bean
	@Primary
	public JsonMapper jsonMapper() {
		return new JsonMapper();
	}

	@Bean
	public RestTemplate tw(RestTemplateBuilder restTemplateBuilder,
						   @Value("${tw.url}") String url,
						   @Value("${tw.token}") String token) {
		return restTemplateBuilder
				.rootUri(url)
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.build();
	}
}
