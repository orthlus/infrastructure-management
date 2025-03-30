package art.aelaort.models.servers.display;

import java.util.Comparator;

public record ClusterAppRow(
		String cluster,
		String image,
		String name,
		String kind,
		String ports,
		String service,
		String schedule,
		String strategy,
		String anotherPorts
) implements Comparable<ClusterAppRow> {

	@Override
	public int compareTo(ClusterAppRow o) {
		return Comparator.comparing(ClusterAppRow::cluster)
				.thenComparing(ClusterAppRow::kind).reversed()
				.thenComparing(ClusterAppRow::name)
				.compare(this, o);
	}
}