package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.UserApi;
import org.gitlab4j.api.models.ImpersonationToken;
import org.gitlab4j.api.models.ImpersonationToken.Scope;

import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.util.Pair;

public class GitLabTestHelper {
	/**
	 * The name of the impersonation token that {@link #createTestUserAndImpersonationToken} creates.
	 * <p>
	 * Production code doesn't create impersonation tokens at all - this exists purely so that the tests can get hold of a token for a user they just created.
	 */
	public static final String TEST_IMPERSONATION_TOKEN_NAME = "catma-test-ipt";

	public static Pair<GitlabManagerRestricted, GitlabManagerPrivileged> createGitLabManagers() throws IOException {
		GitlabManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
		String impersonationToken = createTestUserAndImpersonationToken(gitlabManagerPrivileged);

		GitlabManagerRestricted gitlabManagerRestricted = new GitlabManagerRestricted(impersonationToken);

		return new Pair<>(gitlabManagerRestricted, gitlabManagerPrivileged);
	}

	/**
	 * Creates a fake CATMA user and mints an API-scoped impersonation token for it, so that a {@link GitlabManagerRestricted} can be instantiated.
	 *
	 * @param gitlabManagerPrivileged the privileged manager to create the user with
	 * @return the raw impersonation token string
	 * @throws IOException if the user or the token couldn't be created
	 */
	public static String createTestUserAndImpersonationToken(GitlabManagerPrivileged gitlabManagerPrivileged) throws IOException {
		int randomUserId = new Random().nextInt(1000);
		String username = String.format("testuser-%s", randomUserId);
		String email = String.format("%s@catma.de", username);
		String name = String.format("Test User %s", randomUserId);

		long userId = gitlabManagerPrivileged.createUser(email, username, null, name);

		// the admin API is the same route that deleteUserAndLocalFiles uses for teardown
		try {
			ImpersonationToken impersonationToken = gitlabManagerPrivileged.getGitLabApi().getUserApi().createImpersonationToken(
					userId,
					TEST_IMPERSONATION_TOKEN_NAME,
					// GitLab ignores anything but the date component and interprets it as UTC
					Date.from(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(2).toInstant()),
					new Scope[] {Scope.API}
			);
			return impersonationToken.getToken();
		}
		catch (GitLabApiException e) {
			throw new IOException(String.format("Failed to create impersonation token for user \"%s\"", username), e);
		}
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
