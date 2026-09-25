package de.catma.repository.git.managers;

import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.GitLabUtils;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import org.apache.commons.lang3.RandomStringUtils;
import org.gitlab4j.api.ExtendedPersonalAccessTokenApi;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.NotificationSettingsApi;
import org.gitlab4j.api.UserApi;
import org.gitlab4j.api.models.*;
import org.gitlab4j.api.models.ImpersonationToken.Scope;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

	/**
	 * The scopes that {@link CATMAPropertyKey#GITLAB_ADMIN_PERSONAL_ACCESS_TOKEN} has to have. <code>api</code> covers nearly everything CATMA does as an
	 * admin; <code>sudo</code> is needed by {@link #createUser}, which acts as the new user to disable their notifications.
	 * <p>
	 * These are the strings GitLab itself uses, rather than a gitlab4j enum - see {@link ExtendedPersonalAccessTokenApi} for why.
	 */
	private static final List<String> REQUIRED_ADMIN_TOKEN_SCOPES = List.of("api", "sudo");

	private final Logger logger = Logger.getLogger(GitlabManagerPrivileged.class.getName());

	private final GitLabApi privilegedGitLabApi;

	public GitlabManagerPrivileged() {
		this.privilegedGitLabApi = newAdminGitLabApi();
		this.privilegedGitLabApi.getUserApi().enableCustomAttributes();
	}

	private static GitLabApi newAdminGitLabApi() {
		return new GitLabApi(
				CATMAPropertyKey.GITLAB_SERVER_URL.getValue(), CATMAPropertyKey.GITLAB_ADMIN_PERSONAL_ACCESS_TOKEN.getValue()
		);
	}

	/**
	 * Checks that the configured admin personal access token is usable, i.e. that it has all of {@link #REQUIRED_ADMIN_TOKEN_SCOPES} and belongs to an
	 * administrator. Both matter: GitLab refuses a sudo request that lacks the scope, and equally one made by a non-admin, so the scope list alone doesn't
	 * establish that the token will work.
	 * <p>
	 * Reports every problem it finds rather than stopping at the first, so that an operator can fix the configuration in one pass.
	 *
	 * @return the problems found, empty if the token is configured correctly
	 * @throws IOException if the check couldn't be carried out at all, e.g. because the GitLab server is unreachable. This is not the same as finding a
	 *         problem: the token may well be fine, we just couldn't establish that. {@link de.catma.servlet.GitLabCapabilitiesCheckServlet} acts on the
	 *         two differently.
	 */
	public static List<String> checkAdminTokenCapabilities() throws IOException {
		try (GitLabApi gitlabApi = newAdminGitLabApi()) {
			List<String> scopes = new ExtendedPersonalAccessTokenApi(gitlabApi).getCurrentTokenScopes();
			User tokenUser = gitlabApi.getUserApi().getCurrentUser();

			List<String> problems = new ArrayList<>();

			List<String> missingScopes = REQUIRED_ADMIN_TOKEN_SCOPES.stream().filter(scope -> !scopes.contains(scope)).toList();
			if (!missingScopes.isEmpty()) {
				problems.add(
						String.format(
								"it is missing the %s scope(s) - it has %s. Scopes can't be added to an existing token, so a replacement has to be created "
										+ "(see doc/SELF-HOSTING.md)",
								String.join(", ", missingScopes),
								scopes.isEmpty() ? "none" : String.join(", ", scopes)
						)
				);
			}

			if (!Boolean.TRUE.equals(tokenUser.getIsAdmin())) {
				problems.add(String.format("it belongs to \"%s\", who is not an administrator", tokenUser.getUsername()));
			}

			return problems;
		}
		catch (GitLabApiException e) {
			throw new IOException("Couldn't check the capabilities of the GitLab admin personal access token", e);
		}
	}

	@Override
	public String createPersonalAccessToken(long userId, String tokenName, LocalDate expiresAt) throws IOException {
		UserApi userApi = privilegedGitLabApi.getUserApi();

		try {
			ImpersonationToken personalAccessToken = userApi.createPersonalAccessToken(
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
		User user = createGitLabUser(email, username, password, publicname);

		// the notification settings endpoints only ever act on the calling user, so the admin has to act as the new user to disable their notifications
		// NB: sudo is set on a short-lived client rather than on privilegedGitLabApi, because setSudoAsId mutates the underlying ApiClient and would
		//     therefore leak the Sudo header onto every other caller of the shared instance
		try (GitLabApi gitlabApi = newAdminGitLabApi()) {
			gitlabApi.setSudoAsId(user.getId());

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

	private User createGitLabUser(String email, String username, String password, String publicname) throws IOException {
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

		try {
			user = userApi.createUser(user, password, false);//do not send a pwd reset link
			return user;
		}
		catch (GitLabApiException e) {
			throw new IOException(e.getMessage(), e); // explicitly using the message from GitLabApiException as it can contain details about validation errors
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
				userApi.changeCustomAttribute(user.getId(), optionalLastLoginAtt.get()); // TODO: why aren't we just passing lastLogin?
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
