package art.aelaort.models.servers.display;

public record ClusterAppRow(String cluster, String image, String name, String kind, String ports, String service, String schedule, String strategy) {
}