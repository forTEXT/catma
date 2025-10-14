package de.catma.repository.git.managers;

import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.GitLabUtils;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.util.Pair;
import org.apache.commons.lang3.RandomStringUtils;
import org.gitlab4j.api.Constants.ImpersonationState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.NotificationSettingsApi;
import org.gitlab4j.api.UserApi;
import org.gitlab4j.api.models.*;
import org.gitlab4j.api.models.PersonalAccessToken.Scope;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitlabManagerPrivileged extends GitlabManagerCommon implements RemoteGitManagerPrivileged {
	private enum CustomAttributeName {
		last_login,
		terms_of_use_consent_given,
	}

	public static final String GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME = "catma-default-ipt";

	private final Logger logger = Logger.getLogger(GitlabManagerPrivileged.class.getName());

	private final GitLabApi privilegedGitLabApi;

	public GitlabManagerPrivileged() {
		this.privilegedGitLabApi = new GitLabApi(
				CATMAPropertyKey.GITLAB_SERVER_URL.getValue(), CATMAPropertyKey.GITLAB_ADMIN_PERSONAL_ACCESS_TOKEN.getValue()
		);
		this.privilegedGitLabApi.getUserApi().enableCustomAttributes();
	}

	@Override
	public Pair<GitUser, String> acquireImpersonationToken(String identifier, String provider, String email, String name) throws IOException {
		User user = acquireUser(identifier, provider, email, name);
		UserApi userApi = privilegedGitLabApi.getUserApi();

		try {
			List<PersonalAccessToken> impersonationTokens = userApi.getImpersonationTokens(
				user.getId(), ImpersonationState.ACTIVE
			);

			// revoke the default token if it exists already
			// we do this because the actual token string is only returned on creation and we don't store it
			for (PersonalAccessToken token : impersonationTokens) {
				if (token.getName().equals(GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME)) {
					userApi.revokeImpersonationToken(user.getId(), token.getId());
					break;
				}
			}
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to revoke existing impersonation token", e);
		}

		String impersonationToken = createImpersonationToken(user.getId(), GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME);

		if (impersonationToken == null) {
			throw new IOException(
					String.format(
							"Failed to acquire impersonation token for user \"%s\". No active impersonation token called \"%s\" can be found.",
							user.getUsername(),
							GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME
					)
			);
		}

		return new Pair<>(new GitUser(user), impersonationToken);
	}

	@Override
	public String createPersonalAccessToken(long userId, String tokenName, LocalDate expiresAt) throws IOException {
		UserApi userApi = privilegedGitLabApi.getUserApi();

		try {
			PersonalAccessToken personalAccessToken = userApi.createPersonalAccessToken(
					userId,
					tokenName,
					// GitLab ignores anything but the date component and interprets it as UTC
					// see related TODO in AccessTokenDialog
					Date.from(expiresAt.atStartOfDay().toInstant(ZoneOffset.UTC)),
					new Scope[] {Scope.READ_API}
			);
			logger.info(String.format("Created personal access token for user with ID %s", userId));
			return personalAccessToken.getToken();
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to create personal access token", e);
		}
	}

	@Override
	public long createUser(String email, String username, String password, String publicname) throws IOException {
		User user = this.createUser(email, username, password, publicname, null);
		String token = this.createImpersonationToken(user.getId(), GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME);

		try (GitLabApi gitlabApi = new GitLabApi(CATMAPropertyKey.GITLAB_SERVER_URL.getValue(), token)) {
			NotificationSettingsApi userNotificationSettingsApi = gitlabApi.getNotificationSettingsApi();
			NotificationSettings globalNotificationSettings = userNotificationSettingsApi.getGlobalNotificationSettings();
			globalNotificationSettings.setLevel(NotificationSettings.Level.DISABLED);
			userNotificationSettingsApi.updateGlobalNotificationSettings(globalNotificationSettings);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to update notification settings for user \"%s\"", user.getUsername()),
					e
			);
		}

		return user.getId();
	}

	/**
	 * Acquires (gets or creates) a GitLab user for third-party logins.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param identifier the third-party unique identifier for the user (combined with <code>provider</code> to form the username)
	 * @param provider the third-party provider name (combined with <code>identifier</code> to form the username)
	 * @param email the user's email address
	 * @param publicName the user's public name
	 * @return a {@link User}
	 * @throws IOException if an error occurs when acquiring the GitLab user
	 */
	private User acquireUser(String identifier, String provider, String email, String publicName) throws IOException {
		UserApi userApi = privilegedGitLabApi.getUserApi();

		String username = identifier + provider;

		try {
			User user = userApi.getUser(username);

			if (user == null) {
				user = createUser(
						email,
						username,
						null, // password will be generated
						publicName,
						provider
				);
			}

			return user;
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to acquire GitLab user", e);
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
	private String createImpersonationToken(long userId, String tokenName) throws IOException {
		UserApi userApi = privilegedGitLabApi.getUserApi();

		try {
			PersonalAccessToken impersonationToken = userApi.createImpersonationToken(
					userId,
					tokenName,
					// GitLab ignores anything but the date component and interprets it as UTC
					// sessions are unlikely to last more than a couple of days (also see session config in web.xml)
					Date.from(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(2).toInstant()),
					new Scope[] {Scope.API}
			);
			return impersonationToken.getToken();
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to create impersonation token", e);
		}
	}

	@Override
	public void modifyUserAttributes(long userId, String name, String password) throws IOException {
		try {
			User user = privilegedGitLabApi.getUserApi().getUser(userId);
			
			BiConsumer<String,Consumer<String>> fExecIfNotNull = (attr,func) -> { 
				if(attr != null) 
					func.accept(attr);
			};
			fExecIfNotNull.accept(name, user::setName);

			this.privilegedGitLabApi.getUserApi().updateUser(user, password);
		} catch(GitLabApiException e){
			throw new IOException("Failed to modify user attributes",e);
		}
	}

	@Override
	public boolean emailOrUsernameExists(String emailOrUsername) throws IOException {
		try {
			return privilegedGitLabApi.getUserApi().findUsers(emailOrUsername)
					.stream().anyMatch(user -> user.getUsername().equals(emailOrUsername) || user.getEmail().equals(emailOrUsername));
		}
		catch(GitLabApiException e) {
			throw new IOException("Failed to check whether user exists", e);
		}
	}

	// it's more convenient to work with the User class internally, which is why this method exists
	private User createUser(String email, String username, String password, String publicname, String provider
			) throws IOException {
		UserApi userApi = privilegedGitLabApi.getUserApi();
		if (password == null) {
			// generate a random password
			password = RandomStringUtils.random(
				12, 0, GitLabUtils.PWD_CHARS.length-1,
				false, false, GitLabUtils.PWD_CHARS, new SecureRandom()
			);
		}

		User user = new User();
		user.setEmail(email);
		user.setUsername(username);
		user.setName(publicname);
		user.setIsAdmin(false);
		user.setSkipConfirmation(true);

		// TODO: remove, this doesn't actually do anything (gitlab4j-api doesn't do anything with the information)
		//       test whether user.setExternUid and user.setProvider have any effect, and if so, whether that results in a valid identity
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
	
	public boolean updateLastLoginAndGetTermsOfUseConsent(de.catma.user.User catmaUser) {
		try {
			UserApi userApi = privilegedGitLabApi.getUserApi();
			
			User user = userApi.getUser(catmaUser.getUserId());
			
			Optional<CustomAttribute> optionalLastLoginAtt = Optional.empty();
			if (user.getCustomAttributes() != null) {
				optionalLastLoginAtt = 
					user.getCustomAttributes().stream()
					.filter(attr -> attr.getKey().equals(CustomAttributeName.last_login.name()))
					.findFirst();
			}
		
			if (!optionalLastLoginAtt.isPresent()) {
				logger.info(String.format("First login of user \"%s\"", user.getUsername()));
				
					userApi.createCustomAttribute(
						user.getId(), 
						CustomAttributeName.last_login.name(), 
						LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
			}
			else {
				CustomAttribute lastLogin = optionalLastLoginAtt.get();
				logger.info(String.format("Last login of user \"%s\" was on %s", user.getUsername(), lastLogin.getValue()));
				lastLogin.setValue(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
				userApi.changeCustomAttribute(user.getId(), optionalLastLoginAtt.get());
			}
			
			if (user.getCustomAttributes() != null) {
				return Boolean.valueOf(user.getCustomAttributes().stream()
					.filter(attr -> attr.getKey().equals(CustomAttributeName.terms_of_use_consent_given.name()))
					.findFirst().orElse(new CustomAttribute().withValue(Boolean.FALSE.toString()))
					.getValue());
			}
			else {
				return false;
			}
			
		} catch (GitLabApiException e) {
			logger.log(Level.SEVERE, "Couldn't access custom attributes", e);
			return false;
		}
	}
	
	public void setTermsOfUseConsentGiven(de.catma.user.User catmaUser, boolean value) {
		try {
			UserApi userApi = privilegedGitLabApi.getUserApi();
			
			User user = userApi.getUser(catmaUser.getUserId());
			
			Optional<CustomAttribute> optionalAttribute = Optional.empty();
			if (user.getCustomAttributes() != null) {
				
				optionalAttribute = 
					user.getCustomAttributes().stream()
						.filter(attr -> attr.getKey().equals(CustomAttributeName.terms_of_use_consent_given.name()))
						.findFirst();
			}
			
			CustomAttribute attr = optionalAttribute
					.orElse(new CustomAttribute()
							.withKey(CustomAttributeName.terms_of_use_consent_given.name())
							.withValue(Boolean.FALSE.toString()));
			attr.setValue(Boolean.valueOf(value).toString());
			userApi.changeCustomAttribute(user.getId(), attr);
		} catch (GitLabApiException e) {
			logger.log(Level.SEVERE, "Couldn't access custom attributes", e);
		}
	}
	
}
