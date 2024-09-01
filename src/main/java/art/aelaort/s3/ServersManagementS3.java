package art.aelaort.s3;

import art.aelaort.S3Params;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static art.aelaort.S3ClientProvider.client;

@Component
@RequiredArgsConstructor
public class ServersManagementS3 {
	private final S3Params serversManagementS3Params;
	@Value("${servers.management.s3.bucket}")
	private String bucket;

	public void uploadIps(String ipsText) {
		try (S3Client client = client(serversManagementS3Params)) {
			RequestBody requestBody = RequestBody.fromString(ipsText);
			client.putObject(PutObjectRequest.builder()
					.bucket(bucket)
					.key("ips.txt")
					.build(), requestBody);
		}
	}

	public String downloadData() {
		try (S3Client client = client(serversManagementS3Params)) {
			return client.getObjectAsBytes(builder -> builder
							.bucket(bucket)
							.key("data.json"))
					.asUtf8String();
		}
	}

	public void uploadData(String jsonStr) {
		try (S3Client client = client(serversManagementS3Params)) {
			RequestBody requestBody = RequestBody.fromString(jsonStr);
			client.putObject(PutObjectRequest.builder()
					.bucket(bucket)
					.key("data.json")
					.build(), requestBody);
		}
	}
}
