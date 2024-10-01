package art.aelaort;

import art.aelaort.exceptions.LocalFileNotFountException;
import art.aelaort.exceptions.SshNotFountFileException;
import art.aelaort.models.ssh.SshServer;
import art.aelaort.ssh.SshClient;
import art.aelaort.utils.Utils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static art.aelaort.utils.Utils.linuxResolve;
import static art.aelaort.utils.Utils.log;

@Component
@RequiredArgsConstructor
public class SshKeyUploader {
	private final SshClient sshClient;
	private final Utils utils;
	@Value("${ssh.keys.dir}")
	private Path keysDir;

	public void uploadSshKey(SshServer sshServer, String keyLocalPath, String user) {
		Path toUpload = keysDir.resolve(keyLocalPath);
		if (Files.notExists(toUpload)) {
			throw new LocalFileNotFountException(toUpload.toString());
		}

		Path tmp = utils.createTmpDir();
		try {
			Path authorizedKeysPath = tmp.resolve("authorized_keys");
			sshClient.downloadFile(authorizedKeysFileByUser(user), authorizedKeysPath, sshServer);
			appendToNewLine(toUpload, authorizedKeysPath);
			sshClient.uploadFileNewName(authorizedKeysPath, authorizedKeysFileByUser(user), sshServer);
			log("append new key to remote authorized_keys (%s)%n", user);
		} catch (SshNotFountFileException e) {
			sshClient.uploadFileNewName(
					toUpload,
					authorizedKeysFileByUser(user),
					sshServer
			);
			log("create new remote authorized_keys (%s)%n", user);
		}
	}

	private void appendToNewLine(Path srcPath, Path toAppendPath) {
		try {
			String oldFile = Files.readString(toAppendPath);
			String newLine = Files.readString(srcPath);
			Files.writeString(toAppendPath, oldFile + "\n" + newLine);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String authorizedKeysFileByUser(String user) {
		return linuxResolve(linuxHomeByUser(user), ".ssh/authorized_keys");
	}

	public String linuxHomeByUser(String user) {
		return user.equals("root") ?
				"/root" :
				"/home/" + user;
	}
}
