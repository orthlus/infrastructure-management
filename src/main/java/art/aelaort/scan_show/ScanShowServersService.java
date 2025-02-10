package art.aelaort.scan_show;

import art.aelaort.ServersManagementService;
import art.aelaort.models.servers.Server;
import art.aelaort.servers.providers.ServerProvider;
import art.aelaort.utils.ExternalUtilities;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class ScanShowServersService {
	private final ServersManagementService serversManagementService;
	private final StringFormattingService stringFormattingService;
	private final ExternalUtilities externalUtilities;
	private final ServerProvider serverProvider;

	/*
	 * download tabby
	 * generate json
	 * save data to local
	 * save data to s3
	 * save ips to s3
	 */
	public void sync() {
		List<Server> servers = serverProvider.scanAndJoinData();
		serversManagementService.saveData(servers);
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
		log(stringFormattingService.getServersTableString(servers));
		log();
		log(stringFormattingService.servicesByServerString(servers));
	}

	public void showYml() {
		List<Server> servers = serverProvider.readLocalJsonData();
		log(stringFormattingService.servicesByServerString(servers));
	}

	public void showTable() {
		List<Server> servers = serverProvider.readLocalJsonData();
		log(stringFormattingService.getServersTableString(servers));
	}
}
