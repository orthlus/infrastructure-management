package art.aelaort.models.servers;

import java.util.List;

public record DirServer(String name, boolean monitoring, List<ServiceDto> services) {
}
