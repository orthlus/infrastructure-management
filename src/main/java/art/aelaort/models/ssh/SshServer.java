package art.aelaort.models.ssh;

public record SshServer(String host, String fullKeyPath, int port, String serverDirName) {
}
