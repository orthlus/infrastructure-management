package art.aelaort.models;

import java.util.List;

public record Server(String name, boolean monitoring, List<Service> services) {
}
