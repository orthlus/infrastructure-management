package art.aelaort.data.parsers;

import art.aelaort.models.servers.DirServer;
import art.aelaort.models.servers.ServiceDto;
import art.aelaort.models.servers.yaml.DockerComposeFile;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class DockerComposeParser {
	private final YAMLMapper yamlMapper;
	@Value("${servers.management.files.monitoring}")
	private String monitoringFile;
	@Value("${servers.management.docker.image.pattern}")
	private String dockerImagePattern;
	private String[] dockerImagePatternSplit;

	@PostConstruct
	private void init() {
		dockerImagePatternSplit = dockerImagePattern.split("%%");
	}

	public DirServer parseDockerYmlFile(Path ymlFile) {
		try {
			Path serverDir = ymlFile.getParent();
			boolean monitoring = serverDir.resolve(monitoringFile).toFile().exists();

			DockerComposeFile file = yamlMapper.readValue(ymlFile.toFile(), DockerComposeFile.class);
			validateDockerServices(file, ymlFile);

			List<ServiceDto> resultServices = new ArrayList<>();
			for (Map.Entry<String, DockerComposeFile.Service> entry : file.getServices().entrySet()) {
				resultServices.add(getServiceDto(ymlFile, entry));
			}

			return DirServer.builder()
					.name(serverDir.getFileName().toString())
					.monitoring(monitoring)
					.services(resultServices)
					.build();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void validateDockerServices(DockerComposeFile file, Path ymlFile) {
		file.getServices()
				.entrySet()
				.stream()
				.filter(e -> e.getValue().getRestart() == null)
				.forEach(e -> log("docker service '%s' - not found 'restart' (%s)\n", e.getKey(), ymlFile));
	}

	private ServiceDto getServiceDto(Path ymlFile, Map.Entry<String, DockerComposeFile.Service> entry) {
		String serviceName = entry.getKey();
		DockerComposeFile.Service service = entry.getValue();
		ServiceDto.ServiceDtoBuilder serviceDtoBuilder = ServiceDto.builder()
				.ymlName(ymlFile.getFileName().toString());

		String containerName = service.getContainerName();
		if (containerName != null) {
			serviceDtoBuilder
					.service(containerName)
					.dockerName(serviceName);
		} else {
			serviceDtoBuilder.service(serviceName);
		}

		String image = service.getImage();
		if (image != null) {
			serviceDtoBuilder.dockerImageName(dockerImageClean(image));
		}

		return serviceDtoBuilder.build();
	}

	private String dockerImageClean(String dockerImage) {
		return dockerImage
				.replace(dockerImagePatternSplit[0], "")
				.replace(dockerImagePatternSplit[1], "");
	}
}
