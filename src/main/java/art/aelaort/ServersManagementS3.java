package art.aelaort;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@RequiredArgsConstructor
public class ServersManagementS3 {
	@Qualifier("serversManagement")
	private final S3Client client;
	@Value("${servers.management.s3.bucket}")
	private String bucket;

	public String downloadData() {
		return client.getObjectAsBytes(builder -> builder
						.bucket(bucket)
						.key("data.json"))
				.asUtf8String();
	}

	public void uploadData(String jsonStr) {
		RequestBody requestBody = RequestBody.fromString(jsonStr);
		client.putObject(PutObjectRequest.builder()
				.bucket(bucket)
				.key("data.json")
				.build(), requestBody);
	}
}
