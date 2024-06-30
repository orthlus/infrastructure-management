package art.aelaort;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.Yaml;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class Config {
	@Bean
	public Yaml yaml() {
		return new Yaml();
	}

	@Bean
	public S3Client tabby(
			@Value("${tabby.s3.access.id}") String id,
			@Value("${tabby.s3.access.key}") String key,
			@Value("${tabby.s3.endpoint}") String url,
			@Value("${tabby.s3.region}") String region
	) {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(id, key);
		return S3Client.builder()
				.region(Region.of(region))
				.endpointOverride(URI.create(url))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.forcePathStyle(true)
				.build();
	}

	@Bean
	public S3Client serversManagement(
			@Value("${servers.management.s3.id}") String id,
			@Value("${servers.management.s3.key}") String key,
			@Value("${tabby.s3.endpoint}") String url,
			@Value("${tabby.s3.region}") String region
	) {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(id, key);
		return S3Client.builder()
				.region(Region.of(region))
				.endpointOverride(URI.create(url))
				.credentialsProvider(StaticCredentialsProvider.create(credentials))
				.forcePathStyle(true)
				.build();
	}
}
