package art.aelaort.models.servers.display;

import java.util.Comparator;

public record ClusterAppRow(
		String cluster,
		String namespace,
		String image,
		String name,
		String kind,
		String imagePullPolicy,
		String ports,
		String service,
		String schedule,
		String strategy,
		Boolean anotherPorts
) implements Comparable<ClusterAppRow> {

	@Override
	public int compareTo(ClusterAppRow o) {
		return Comparator.comparing(ClusterAppRow::cluster)
				.thenComparing(ClusterAppRow::kind).reversed()
				.thenComparing(ClusterAppRow::name)
				.compare(this, o);
	}
}