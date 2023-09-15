package de.catma.repository.git.managers;

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

import org.apache.commons.lang3.RandomStringUtils;
import org.gitlab4j.api.Constants.ImpersonationState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.NotificationSettingsApi;
import org.gitlab4j.api.UserApi;
import org.gitlab4j.api.models.CustomAttribute;
import org.gitlab4j.api.models.Identity;
import org.gitlab4j.api.models.PersonalAccessToken;
import org.gitlab4j.api.models.PersonalAccessToken.Scope;
import org.gitlab4j.api.models.NotificationSettings;
import org.gitlab4j.api.models.User;

import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.GitlabUtils;
import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.util.Pair;

public class GitlabManagerPrivileged extends GitlabManagerCommon implements IRemoteGitManagerPrivileged {
	

	private enum CustomAttributeName {
		last_login,
		terms_of_use_consent_given,
	}
	
	public static final String GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME = "catma-default-ipt";

	private final GitLabApi privilegedGitLabApi = new GitLabApi(
			 CATMAPropertyKey.GitLabServerUrl.getValue(), CATMAPropertyKey.GitLabAdminPersonalAccessToken.getValue()
	);
	
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	
	public GitlabManagerPrivileged() {
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
			logger.info(String.format("Created personal access token for user with ID %1$s.", userId));
			return personalAccessToken.getToken();
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to create personal access token", e);
		}
	}

	@Override
	public long createUser(String email, String username, String password,
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
				logger.info(String.format("First Login of %1$s", user.getUsername()));
				
					userApi.createCustomAttribute(
						user.getId(), 
						CustomAttributeName.last_login.name(), 
						LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
			}
			else {
				CustomAttribute lastLogin = optionalLastLoginAtt.get();
				logger.info(String.format("Last Login of %1$s was %2$s.", user.getUsername(), lastLogin.getValue()));
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
			logger.log(Level.SEVERE, "Could access custom attributes", e);
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
			logger.log(Level.SEVERE, "Could access custom attributes", e);
		}
	}
	
}
