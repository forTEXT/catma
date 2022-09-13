package de.catma.repository.git;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.gitlab4j.api.UserApi;

import com.google.common.eventbus.EventBus;

import de.catma.backgroundservice.BackgroundService;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.util.Pair;

public class GitlabTestHelper {
	
	public static Pair<GitlabManagerRestricted, GitlabManagerPrivileged> createGitlabManagers() throws IOException {
		// create a fake CATMA user which we'll use to instantiate GitlabManagerRestricted (using the corresponding impersonation token) & JGitRepoManager
		Integer randomUserId = Integer.parseInt(RandomStringUtils.randomNumeric(3));
		String username = String.format("testuser-%s", randomUserId);
		String email = String.format("%s@catma.de", username);
		String name = String.format("Test User %s", randomUserId);

		GitlabManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
		String impersonationToken = gitlabManagerPrivileged.acquireImpersonationToken(username, "catma", email, name).getSecond();

		EventBus mockEventBus = mock(EventBus.class);
		BackgroundService mockBackgroundService = mock(BackgroundService.class);
		GitlabManagerRestricted gitlabManagerRestricted = new GitlabManagerRestricted(mockEventBus, mockBackgroundService, impersonationToken);

		return new Pair<>(gitlabManagerRestricted, gitlabManagerPrivileged);
	}

	public static void deleteUserAndTraces(
			GitlabManagerRestricted gitlabManagerRestricted, GitlabManagerPrivileged gitlabManagerPrivileged) throws Exception {
		UserApi userApi = gitlabManagerPrivileged.getGitLabApi().getUserApi();
		userApi.deleteUser(gitlabManagerRestricted.getUser().getUserId(), true);
		FileUtils.deleteDirectory(
				Paths.get(new File(CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue()).toURI())
				.resolve(gitlabManagerRestricted.getUser().getIdentifier())
				.toFile());		
	}
}
