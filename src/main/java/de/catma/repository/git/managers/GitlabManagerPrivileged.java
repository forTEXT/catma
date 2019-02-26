package de.catma.repository.git.managers;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Identity;
import org.gitlab4j.api.models.User;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.GitlabUtils;
import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.repository.git.managers.gitlab4j_api_custom.CustomUserApi;
import de.catma.repository.git.managers.gitlab4j_api_custom.models.ImpersonationToken;
import de.catma.user.UserProperty;
import de.catma.util.Pair;

public class GitlabManagerPrivileged implements IRemoteGitManagerPrivileged {
	
	public static final String GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME = "catma-default-ipt";

	private final GitLabApi privilegedGitLabApi = new GitLabApi(
			 RepositoryPropertyKey.GitLabServerUrl.getValue(), RepositoryPropertyKey.GitLabAdminPersonalAccessToken.getValue()
	);
	
	private final Logger logger = Logger.getLogger(this.getClass().getName());

	
	
	@Override
	public Pair<GitUser, String> acquireImpersonationToken(String identifier, String provider, String email, String name)
			throws IOException {
		Pair<User, Boolean> userWasCreatedPair = this.acquireUser(identifier,provider,email,name);
		User user = userWasCreatedPair.getFirst();
		Boolean wasCreated = userWasCreatedPair.getSecond();

		String impersonationToken = null;

		if (wasCreated) {
			// if the user was newly created, we need to create an impersonation token
			impersonationToken = this.createImpersonationToken(
				user.getId(), GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME
			);
		}
		else {
			// if the user already existed, we expect there to be an impersonation token that we can
			// fetch
			CustomUserApi customUserApi = new CustomUserApi(this.privilegedGitLabApi);

			try {
				List<ImpersonationToken> impersonationTokens = customUserApi.getImpersonationTokens(
					user.getId(), "active"
				);

				// strictly speaking a user shouldn't have more than one impersonation token if
				// created through this class, but this at least allows for special cases
				for (ImpersonationToken token : impersonationTokens) {
					if (token.name.equals(GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME)) {
						impersonationToken = token.token;
						break;
					}
				}
			}
			catch (GitLabApiException e) {
				throw new IOException(
					"Failed to acquire impersonation token", e
				);
			}
		}

		if (impersonationToken == null) {
			String errorMessage = String.format(
				"Failed to acquire impersonation token for CATMA with identifier `%s`. " +
				"This could be because the associated GitLab user ID `%s` does not have an " +
				"active impersonation token called `%s`, or has so many tokens that " +
				"CustomUserApi.getImpersonationTokens(int, String) is not returning it.",
				identifier,
				user.getId(), GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME
			);
			throw new IOException(errorMessage);
		}
		
		Pair<GitUser, String> retVal = new Pair<>(new GitUser(user), impersonationToken);

		return retVal;
	}
	
	@Override
	public int createUser(String email, String identifier, @Nullable String password,
			   String name, @Nullable Boolean isAdmin)
					   throws IOException {
		User user = this._createUser(email, identifier, password, name, null, isAdmin);
		this.createImpersonationToken(user.getId(),GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME);
		return user.getId();
	}
	
	/**
	 * Acquires (gets or creates) a GitLab user for the supplied <code>catmaUser</code>.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param userIdentification 
	 * @return a {@link Pair} object with the first value being a {@link User} object and the second
	 *         value being a boolean indicating whether or not the user was newly created
	 * @throws IOException if something went wrong while acquiring the GitLab
	 *         user
	 */
	private Pair<User, Boolean> acquireUser(String identifier, String provider, String email, String name)
			throws IOException {
		CustomUserApi customUserApi = new CustomUserApi(this.privilegedGitLabApi);

		Boolean userCreated = false;

		try {
			User user = customUserApi.getUser(identifier,provider);

			if (user == null) {

				user = this._createUser(email,
						identifier,
						null,
						name,
						provider,
						null
				);
				userCreated = true;
			}

			return new Pair<>(user, userCreated);
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
		CustomUserApi customUserApi = new CustomUserApi(this.privilegedGitLabApi);

		try {
			ImpersonationToken impersonationToken = customUserApi.createImpersonationToken(
				userId, tokenName, null, null
			);
			return impersonationToken.token;
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to create impersonation token", e);
		}
	}
	
	@Override
	public boolean existsUserOrEmail(String usernameOrEmail) throws IOException {
		try {
			List<User> userPager = this.privilegedGitLabApi.getUserApi().findUsers(usernameOrEmail);
			return ! userPager.isEmpty() ;
		} catch(GitLabApiException e){
			throw new IOException("failed to check for username",e);
		}
	}
	
	// it's more convenient to work with the User class internally, which is why this method exists
		private User _createUser(String email, String identifier, @Nullable String password, String name, String provider,
								@Nullable Boolean isAdmin) throws IOException {
			CustomUserApi userApi = new CustomUserApi(privilegedGitLabApi);
			if (password == null) {
				// generate a random password
				password = RandomStringUtils.random(
					12, 0, GitlabUtils.PWD_CHARS.length-1,
					false, false, GitlabUtils.PWD_CHARS, new SecureRandom()
				);
			}

			if (isAdmin == null) {
				isAdmin = false;
			}
			User user = new User();
			user.setEmail(email);
			user.setUsername(identifier);
			user.setName(name);
			user.setIsAdmin(isAdmin);
			
			if (provider != null) {
				Identity identity = new Identity();
				identity.setExternUid(identifier);
				identity.setProvider(provider);
				
				user.setIdentities(Collections.singletonList(identity));
			}
			
			try {
				user = userApi.createUser(user, password, null);
				return user;
			}
			catch (GitLabApiException e) {
				throw new IOException("Failed to create user", e);
			}
		}

}
