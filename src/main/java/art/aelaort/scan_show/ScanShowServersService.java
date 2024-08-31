package art.aelaort.scan_show;

import art.aelaort.ServersManagementService;
import art.aelaort.models.servers.Server;
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

	/*
	 * scan dirs
	 * download tabby
	 * scan tabby
	 * print
	 */
	public void scan() {
		List<Server> servers = serversManagementService.scanAndJoinData();
		log(stringFormattingService.getServersTableString(servers));
		log();
		log(stringFormattingService.servicesByServerFullTreeString(servers));
	}

	public void scanTable() {
		List<Server> servers = serversManagementService.scanAndJoinData();
		log(stringFormattingService.getServersTableString(servers));
	}

	public void scanTree() {
		List<Server> servers = serversManagementService.scanAndJoinData();
		log(stringFormattingService.servicesByServerFullTreeString(servers));
	}

	/*
	 * download tabby
	 * generate json
	 * save data to local
	 * save data to s3
	 * save ips to s3
	 */
	public void sync() {
		List<Server> servers = serversManagementService.scanAndJoinData();
		serversManagementService.saveData(servers);
		serversManagementService.saveIps(servers);
		log("sync done");
	}

	public void syncAll() {
		sync();
		externalUtilities.ydSync();
	}

	/*
	 * read local json
	 * print table
	 * print tree
	 */
	public void show() {
		List<Server> servers = serversManagementService.readLocalJsonData();
		log(stringFormattingService.getServersTableString(servers));
		log();
		log(stringFormattingService.servicesByServerFullTreeString(servers));
	}

	public void showTree() {
		List<Server> servers = serversManagementService.readLocalJsonData();
		log(stringFormattingService.servicesByServerFullTreeString(servers));
	}

	public void showTable() {
		List<Server> servers = serversManagementService.readLocalJsonData();
		log(stringFormattingService.getServersTableString(servers));
	}
}
