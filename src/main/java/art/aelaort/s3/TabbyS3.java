package art.aelaort.s3;

import art.aelaort.S3Params;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

import static art.aelaort.S3ClientProvider.client;


@Component
@RequiredArgsConstructor
public class TabbyS3 {
	private final S3Params tabbyS3Params;
	@Value("${tabby.s3.bucket}")
	private String bucket;
	@Value("${tabby.s3.key}")
	private String key;

	public String download() {
		try (S3Client client = client(tabbyS3Params)) {
			return client.getObjectAsBytes(builder -> builder
							.bucket(bucket)
							.key(key))
					.asUtf8String();
		}
	}
}
