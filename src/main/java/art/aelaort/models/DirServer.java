package art.aelaort.models;

import java.util.List;

public record DirServer(String name, boolean monitoring, List<Service> services) {
}
