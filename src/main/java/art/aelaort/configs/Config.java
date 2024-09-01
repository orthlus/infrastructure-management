package art.aelaort.configs;

import art.aelaort.DefaultS3Params;
import art.aelaort.S3Params;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class Config {
	@Bean
	public XmlMapper xmlMapper() {
		return new XmlMapper();
	}

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
	public S3Params tabbyS3Params(
			@Value("${tabby.s3.access.id}") String id,
			@Value("${tabby.s3.access.key}") String key,
			@Value("${tabby.s3.endpoint}") String endpoint,
			@Value("${tabby.s3.region}") String region
	) {
		return new DefaultS3Params(id, key, endpoint, region);
	}

	@Bean
	public S3Params serversManagementS3Params(
			@Value("${servers.management.s3.id}") String id,
			@Value("${servers.management.s3.key}") String key,
			@Value("${tabby.s3.endpoint}") String endpoint,
			@Value("${tabby.s3.region}") String region
	) {
		return new DefaultS3Params(id, key, endpoint, region);
	}
}
