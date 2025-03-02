package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.gitlab4j.api.UserApi;

import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.util.Pair;

public class GitLabTestHelper {
	public static Pair<GitlabManagerRestricted, GitlabManagerPrivileged> createGitLabManagers() throws IOException {
		// create a fake CATMA user which we'll use to instantiate GitlabManagerRestricted
		Integer randomUserId = new Random().nextInt(1000);
		String username = String.format("testuser-%s", randomUserId);
		String email = String.format("%s@catma.de", username);
		String name = String.format("Test User %s", randomUserId);

		GitlabManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
		String impersonationToken = gitlabManagerPrivileged.acquireImpersonationToken(username, "catma", email, name).getSecond();

		GitlabManagerRestricted gitlabManagerRestricted = new GitlabManagerRestricted(impersonationToken);

		return new Pair<>(gitlabManagerRestricted, gitlabManagerPrivileged);
	}

	public static void deleteUserAndLocalFiles(
			GitlabManagerRestricted gitlabManagerRestricted,
			GitlabManagerPrivileged gitlabManagerPrivileged
	) throws Exception {
		UserApi userApi = gitlabManagerPrivileged.getGitLabApi().getUserApi();
		userApi.deleteUser(gitlabManagerRestricted.getUser().getUserId(), true);

		FileUtils.deleteDirectory(
				Paths.get(new File(CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue()).toURI())
						.resolve(gitlabManagerRestricted.getUser().getIdentifier())
						.toFile()
		);
	}
}
