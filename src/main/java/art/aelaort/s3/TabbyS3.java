package art.aelaort.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;


@Component
@RequiredArgsConstructor
public class TabbyS3 {
	@Qualifier("tabby")
	private final S3Client client;
	@Value("${tabby.s3.bucket}")
	private String bucket;
	@Value("${tabby.s3.key}")
	private String key;

	public String download() {
		return client.getObjectAsBytes(builder -> builder
						.bucket(bucket)
						.key(key))
				.asUtf8String();
	}
}
