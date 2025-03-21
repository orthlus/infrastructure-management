package art.aelaort.scan_show;

import art.aelaort.ServersManagementService;
import art.aelaort.build.JobsProvider;
import art.aelaort.models.build.Job;
import art.aelaort.models.servers.K8sCluster;
import art.aelaort.models.servers.Server;
import art.aelaort.servers.providers.K8sClusterProvider;
import art.aelaort.servers.providers.ServerProvider;
import art.aelaort.utils.ExternalUtilities;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ScanShowServersService {
	private final ServersManagementService serversManagementService;
	private final StringFormattingService stringFormattingService;
	private final ExternalUtilities externalUtilities;
	private final ServerProvider serverProvider;
	private final JobsProvider jobsProvider;
	private final K8sClusterProvider k8sClusterProvider;

	/*
	 * download tabby
	 * generate json
	 * save data to local
	 * save data to s3
	 * save ips to s3
	 */
	public void sync() {
		List<Server> servers = serverProvider.scanAndJoinData();
		List<K8sCluster> clusters = k8sClusterProvider.getClusters();
		serversManagementService.saveData(servers, clusters);
		serversManagementService.saveIps(servers);
		log("sync done");
	}

	public void syncAll() {
		sync();
		externalUtilities.dirSync();
	}

	/*
	 * read local json
	 * print table servers
	 * print table services
	 */
	public void show() {
		List<Server> servers = serverProvider.readLocalJsonData();
		Map<String, Job> jobs = jobsProvider.getJobsMapByName();
		log(stringFormattingService.getServersTableString(servers));
		log();
		log(stringFormattingService.servicesByServerString(servers, jobs));
		log();
		showK8s();
	}

	public void showYml() {
		List<Server> servers = serverProvider.readLocalJsonData();
		Map<String, Job> jobs = jobsProvider.getJobsMapByName();
		log(stringFormattingService.servicesByServerString(servers, jobs));
		log();
		showK8s();
	}

	public void showTable() {
		List<Server> servers = serverProvider.readLocalJsonData();
		log(stringFormattingService.getServersTableString(servers));
	}

	public void showK8s() {
		List<K8sCluster> clusters = k8sClusterProvider.getClustersFromLocalConfig();
		log(stringFormattingService.getK8sTableString(clusters));
	}
}
