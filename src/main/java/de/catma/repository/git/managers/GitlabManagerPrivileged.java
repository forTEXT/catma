package de.catma.repository.git.managers;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.apache.commons.lang3.RandomStringUtils;
import org.gitlab4j.api.Constants.ImpersonationState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.NotificationSettingsApi;
import org.gitlab4j.api.UserApi;
import org.gitlab4j.api.models.Identity;
import org.gitlab4j.api.models.ImpersonationToken;
import org.gitlab4j.api.models.ImpersonationToken.Scope;
import org.gitlab4j.api.models.NotificationSettings;
import org.gitlab4j.api.models.User;

import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.GitlabUtils;
import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.util.Pair;

public class GitlabManagerPrivileged extends GitlabManagerCommon implements IRemoteGitManagerPrivileged {
	
	public static final String GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME = "catma-default-ipt";

	private final GitLabApi privilegedGitLabApi = new GitLabApi(
			 CATMAPropertyKey.GitLabServerUrl.getValue(), CATMAPropertyKey.GitLabAdminPersonalAccessToken.getValue()
	);
	
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	
	
	@Override
	public Pair<GitUser, String> acquireImpersonationToken(String identifier, String provider, String email, String name)
			throws IOException {
		User user = this.acquireUser(identifier,provider,email,name);
		
		UserApi customUserApi = this.privilegedGitLabApi.getUserApi();

		try {
			List<ImpersonationToken> impersonationTokens = customUserApi.getImpersonationTokens(
				user.getId(), ImpersonationState.ACTIVE
			);

			// revoke the default token if it exists actively
			for (ImpersonationToken token : impersonationTokens) {
				if (token.getName().equals(GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME)) {
					privilegedGitLabApi.getUserApi().revokeImpersonationToken(user.getId(), token.getId());
					break;
				}
			}
		}
		catch (GitLabApiException e) {
			throw new IOException(
				"Failed to revoke existing impersonation token", e
			);
		}

		String impersonationToken = this.createImpersonationToken(
			user.getId(), GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME
		);

		if (impersonationToken == null) {
			String errorMessage = String.format(
				"Failed to acquire impersonation token for CATMA with identifier `%s`. " +
				"The creation of the token the associated GitLab user ID `%s` failed, no " +
				"active impersonation token called `%s` can be found!",
				identifier,
				user.getId(), GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME
			);
			throw new IOException(errorMessage);
		}
		
		Pair<GitUser, String> retVal = new Pair<>(new GitUser(user), impersonationToken);

		return retVal;
	}
	
	@Override
	public int createUser(String email, String username, String password,
			   String publicname)
					   throws IOException {
		User user = this.createUser(email, username, password, publicname, null);
		String token = this.createImpersonationToken(user.getId(),GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME);
		NotificationSettingsApi userNotificationSettingsApi = 
			new GitLabApi(CATMAPropertyKey.GitLabServerUrl.getValue(), token).getNotificationSettingsApi();
		try {
			NotificationSettings settings = userNotificationSettingsApi.getGlobalNotificationSettings();
			settings.setLevel(NotificationSettings.Level.DISABLED);
			userNotificationSettingsApi.updateGlobalNotificationSettings(settings);
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to update notification settings for " + user, e);
		}
		return user.getId();
	}
	
	/**
	 * Acquires (gets or creates) a GitLab user for the supplied <code>catmaUser</code>.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param userIdentification 
	 * @return the user
	 * @throws IOException if something went wrong while acquiring the GitLab
	 *         user
	 */
	private User acquireUser(String identifier, String provider, String email, String name)
			throws IOException {
		UserApi customUserApi = this.privilegedGitLabApi.getUserApi();

		try {
			User user = customUserApi.getUser(identifier + provider);

			if (user == null) {

				user = this.createUser(
						email,
						identifier + provider, // username
						null, //password will be generated
						name, //public name
						provider
				);
			}

			return user;
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to acquire remote user", e);
		}
	}
	
	/**
	 * Creates a new impersonation token for the GitLab user identified by <code>userId</code>.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param userId the ID of the user for which to create the impersonation token
	 * @param tokenName the name of the impersonation token to create
	 * @return the new token
	 * @throws IOException if something went wrong while creating the
	 *         impersonation token
	 */
	private String createImpersonationToken(int userId, String tokenName)
			throws IOException {
		UserApi userApi = this.privilegedGitLabApi.getUserApi();

		try {
			ImpersonationToken impersonationToken = userApi.createImpersonationToken(
				userId, tokenName, null, new Scope[] {Scope.API}
			);
			return impersonationToken.getToken();
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to create impersonation token", e);
		}
	}
	
	@Override
	public void modifyUserAttributes(int userId, String name, String password) throws IOException {
		try {
			User user = privilegedGitLabApi.getUserApi().getUser(userId);
			
			BiConsumer<String,Consumer<String>> fExecIfNotNull = (attr,func) -> { 
				if(attr != null) 
					func.accept(attr);
			};
			fExecIfNotNull.accept(name, user::setName);

			this.privilegedGitLabApi.getUserApi().updateUser(user, password);
		} catch(GitLabApiException e){
			throw new IOException("failed to check for username",e);
		}
	}
	
	@Override
	public boolean existsUserOrEmail(String usernameOrEmail) throws IOException {
		try {
			List<User> userPager = this.privilegedGitLabApi.getUserApi().findUsers(usernameOrEmail);
			return userPager.stream()
			.filter(u -> usernameOrEmail.equals(u.getUsername()) || usernameOrEmail.equals(u.getEmail()))
			.count() > 0;
		} catch(GitLabApiException e){
			throw new IOException("Failed to validate username and email!",e);
		}
	}
	
	// it's more convenient to work with the User class internally, which is why this method exists
	private User createUser(String email, String username, String password, String publicname, String provider
			) throws IOException {
		UserApi userApi = privilegedGitLabApi.getUserApi();
		if (password == null) {
			// generate a random password
			password = RandomStringUtils.random(
				12, 0, GitlabUtils.PWD_CHARS.length-1,
				false, false, GitlabUtils.PWD_CHARS, new SecureRandom()
			);
		}

		User user = new User();
		user.setEmail(email);
		user.setUsername(username);
		user.setName(publicname);
		user.setIsAdmin(false);
		user.setSkipConfirmation(true);
		
		if (provider != null) {
			Identity identity = new Identity();
			identity.setExternUid(username);
			identity.setProvider(provider);
			
			user.setIdentities(Collections.singletonList(identity));
		}
		
		try {
			user = userApi.createUser(user, password, false);//do not send a pwd reset link
			return user;
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to create user", e);
		}
	}

	@Override
	public GitLabApi getGitLabApi() {
		return privilegedGitLabApi;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}
		
}
