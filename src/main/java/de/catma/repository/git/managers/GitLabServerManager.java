package de.catma.repository.git.managers;

import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Identity;
import org.gitlab4j.api.models.Namespace;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;

import de.catma.Pager;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.project.ProjectReference;
import de.catma.repository.git.GitMember;
import de.catma.repository.git.GitProjectManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.repository.git.managers.gitlab4j_api_custom.CustomUserApi;
import de.catma.repository.git.managers.gitlab4j_api_custom.models.ImpersonationToken;
import de.catma.user.UserProperty;
import de.catma.util.Pair;

public class GitLabServerManager implements IRemoteGitServerManager {
	private Logger logger = Logger.getLogger(this.getClass().getName());

	private final String gitLabAdminPersonalAccessToken;
	private final String gitLabServerUrl;

	private final GitLabApi adminGitLabApi;
	private final GitLabApi userGitLabApi;

	private final User gitLabUser;
	private final String gitLabUserImpersonationToken;

//	static final String GITLAB_USER_EMAIL_ADDRESS_FORMAT = "catma-user-%s@catma.de";
	static final String GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME = "catma-default-ipt";

	private static final char[] PWD_CHARS = (
		"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
		"0123456789~`!@#$%^&*()-_=+[{]}\\|;:\'\",<.>/?"
	).toCharArray();

	/**
	 * Creates a new GitLabServerManager instance.
	 * <p>
	 * Unless stated otherwise, all actions are performed as the <code>catmaUser</code> supplied
	 * (technically, as a GitLab user that represents the CATMA user).
	 *
	 * @param catmaProperties a {@link Properties} object
	 * @param catmaUser a {@link de.catma.user.User} object
	 * @throws IOException if something went wrong during instantiation
	 */
	public GitLabServerManager(Map<String, String> userIdentification)
			throws IOException {
		this.gitLabAdminPersonalAccessToken = RepositoryPropertyKey.GitLabAdminPersonalAccessToken.getValue();
		
		this.gitLabServerUrl = RepositoryPropertyKey.GitLabServerUrl.getValue();

		this.adminGitLabApi = new GitLabApi(
			this.gitLabServerUrl, this.gitLabAdminPersonalAccessToken
		);

		Pair<User, String> userRawTokenPair = this.acquireImpersonationToken(userIdentification);
		this.gitLabUser = userRawTokenPair.getFirst();
		this.gitLabUserImpersonationToken = userRawTokenPair.getSecond();

		this.userGitLabApi = new GitLabApi(this.gitLabServerUrl, this.gitLabUserImpersonationToken);
	}

	public String getGitLabServerUrl() {
		return this.gitLabServerUrl;
	}

	public GitLabApi getAdminGitLabApi() {
		return this.adminGitLabApi;
	}

	public GitLabApi getUserGitLabApi() {
		return this.userGitLabApi;
	}

	public User getGitLabUser() {
		return this.gitLabUser;
	}

	// needed for testing but also when gitlab runs on port other than 80
	private String getGitLabServerUrl(String url) {
		try {
			URL currentUrl = new URL(url);
			URL gitLabServerUrl = new URL(this.gitLabServerUrl);
			URL newUrl = new URL(
					gitLabServerUrl.getProtocol(), gitLabServerUrl.getHost(), gitLabServerUrl.getPort(),
					currentUrl.getFile()
			);
			return newUrl.toString();
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Creates a new remote repository with the <code>name</code> and <code>path</code> supplied.
	 *
	 * @param name the name of the repository to create
	 * @param path the path of the repository to create
	 * @return a {@link CreateRepositoryResponse} object containing the repository ID and HTTP URL
	 * @throws IOException if something went wrong while creating the remote
	 *         repository
	 */
	@Override
	public CreateRepositoryResponse createRepository(String name, @Nullable String path)
			throws IOException {
		ProjectApi projectApi = this.userGitLabApi.getProjectApi();

		Project project = new Project();
		project.setName(name);

		if (StringUtils.isNotEmpty(path)) {
			project.setPath(path);
		}

		try {
			project = projectApi.createProject(project);
			return new CreateRepositoryResponse(
				null, project.getId(),
				this.getGitLabServerUrl(project.getHttpUrlToRepo())
			);
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to create remote Git repository", e);
		}
	}

	/**
	 * Creates a new remote repository with the <code>name</code> and <code>path</code> supplied
	 * within the group specified by the <code>groupId</code>.
	 *
	 * @param name the name of the repository to create
	 * @param path the path of the repository to create
	 * @param groupPath the path of the group within which to create the repository
	 * @return a {@link CreateRepositoryResponse} object containing the repository ID and HTTP URL
	 * @throws IOException if something went wrong while creating the remote
	 *         repository
	 */
	@Override
	public CreateRepositoryResponse createRepository(
			String name, @Nullable String path, String groupPath)
			throws IOException {
		GroupApi groupApi = this.userGitLabApi.getGroupApi();
		ProjectApi projectApi = this.userGitLabApi.getProjectApi();

		try {
			Group group = groupApi.getGroup(groupPath);

			Namespace namespace = new Namespace();
			namespace.setId(group.getId());

			Project project = new Project();
			project.setName(name);
			project.setNamespace(namespace);

			if (StringUtils.isNotEmpty(path)) {
				project.setPath(path);
			}

			project = projectApi.createProject(project);
			return new CreateRepositoryResponse(
				groupPath, project.getId(),
				this.getGitLabServerUrl(project.getHttpUrlToRepo())
			);
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to create remote Git repository", e);
		}
	}

	/**
	 * Deletes an existing remote repository identified by <code>repositoryId</code>.
	 *
	 * @param repositoryId the ID of the repository to delete
	 * @throws IOException if something went wrong while deleting the remote
	 *         repository
	 */
	@Override
	public void deleteRepository(int repositoryId) throws IOException {
		ProjectApi projectApi = this.userGitLabApi.getProjectApi();

		try {
			projectApi.deleteProject(repositoryId);
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to delete remote Git repository", e);
		}
	}

	/**
	 * Creates a new remote group with the <code>name</code>, <code>path</code> and
	 * <code>description</code> supplied.
	 *
	 * @param name the name of the group to create
	 * @param path the path of the group to create
	 * @param description the description of the group to create
	 * @return the new group path
	 * @throws IOException if something went wrong while creating the remote
	 *         group
	 */
	@Override
	public String createGroup(String name, String path, @Nullable String description)
			throws IOException {
		GroupApi groupApi = this.userGitLabApi.getGroupApi();

		try {
			// none of the addGroup overloads accept a Group object parameter
			groupApi.addGroup(
				name, path, description,
				null, null, null, null,
				null, null, null
			);

			return path;
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to create remote group", e);
		}
	}

	/**
	 * Gets the names of all repositories in the remote group identified by <code>path</code>.
	 *
	 * @param path the path of the group whose repository names you want to get
	 * @return a {@link List<String>} of names
	 * @throws IOException if something went wrong while fetching the repository
	 *         names of the remote group
	 */
	@Override
	public List<String> getGroupRepositoryNames(String path)
			throws IOException {
		GroupApi groupApi = this.userGitLabApi.getGroupApi();

		try {
			Group group = groupApi.getGroup(path);
			List<Project> projects = group.getProjects();
			return projects.stream().map(Project::getName).collect(Collectors.toList());
		}
		catch (GitLabApiException e) {
			throw new IOException(
				"Failed to get repository names for group", e
			);
		}
	}

	/**
	 * Deletes an existing remote group identified by <code>path</code>.
	 * <p>
	 * NB: Also deletes any child repositories!
	 *
	 * @param path the path of the group to delete
	 * @throws IOException if something went wrong while deleting the remote
	 *         group
	 */
	@Override
	public void deleteGroup(String path) throws IOException {
		GroupApi groupApi = this.userGitLabApi.getGroupApi();

		try {
			Group group = groupApi.getGroup(path);
			groupApi.deleteGroup(group);
		}
		catch (GitLabApiException e) {
			//TODO: Nasty workaround, but it's not fixed in gitlab4j yet.
			// A switch to a new gitlab4j requires jaxb 2.3.0 to work. This requires jetty 9.4 to work, which is 
			// broken in the current elcipse jetty plugin
			if(e.getHttpStatus() == 202){  // Async operation indicated by HTTP ACCEPT 202. wait till finished
				for(int i = 0;i < 10; i++ ){
					logger.info("gitlab: async delete operation detected, waiting 150msec per round. round: " + i );
					try {
						Thread.sleep(50);
						List<Group> res = groupApi.getGroups(path);
						if(res.isEmpty()){
							return;
						}
					} catch (GitLabApiException e1) {
						continue; //NOOP
					} catch (InterruptedException e1) {
						continue; //NOOP
					}
				}
				throw new IOException("Failed to delete remote group", e);
			}else {
				throw new IOException("Failed to delete remote group", e);
			}
		}
	}

	// ***** methods below perform their actions as a GitLab admin *****

	/**
	 * Acquires (gets or creates) a GitLab impersonation token for the supplied
	 * <code>catmaUser</code>.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param userIdentification 
	 * @return a {@link Pair} object with the first value being a {@link User} object and the second
	 *         value being the raw impersonation token string
	 * @throws IOException if something went wrong while acquiring the GitLab
	 *         impersonation token
	 */
	private Pair<User, String> acquireImpersonationToken(Map<String, String> userIdentification)
			throws IOException {
		Pair<User, Boolean> userWasCreatedPair = this.acquireUser(userIdentification);
		User user = userWasCreatedPair.getFirst();
		Boolean wasCreated = userWasCreatedPair.getSecond();

		String impersonationToken = null;

		if (wasCreated) {
			// if the user was newly created, we need to create an impersonation token
			impersonationToken = this.createImpersonationToken(
				user.getId(), GitLabServerManager.GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME
			);
		}
		else {
			// if the user already existed, we expect there to be an impersonation token that we can
			// fetch
			CustomUserApi customUserApi = new CustomUserApi(this.adminGitLabApi);

			try {
				List<ImpersonationToken> impersonationTokens = customUserApi.getImpersonationTokens(
					user.getId(), "active"
				);

				// strictly speaking a user shouldn't have more than one impersonation token if
				// created through this class, but this at least allows for special cases
				for (ImpersonationToken token : impersonationTokens) {
					if (token.name.equals(
							GitLabServerManager.GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME)) {
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
				userIdentification.get(UserProperty.identifier.name()),
				user.getId(), GitLabServerManager.GITLAB_DEFAULT_IMPERSONATION_TOKEN_NAME
			);
			throw new IOException(errorMessage);
		}

		Pair<User, String> retVal = new Pair<>(user, impersonationToken);

		return retVal;
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
	private Pair<User, Boolean> acquireUser(Map<String, String> userIdentification)
			throws IOException {
		CustomUserApi customUserApi = new CustomUserApi(this.adminGitLabApi);

		Boolean userCreated = false;

		try {
			User user = customUserApi.getUser(
					userIdentification.get(UserProperty.identifier.name()), 
					userIdentification.get(UserProperty.provider.name()));

			if (user == null) {

				user = this._createUser(
						userIdentification.get(UserProperty.email.name()), 
						userIdentification.get(UserProperty.identifier.name()),
						null,
						userIdentification.get(UserProperty.name.name()),
						userIdentification.get(UserProperty.provider.name()),
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

	// it's more convenient to work with the User class internally, which is why this method exists
	private User _createUser(String email, String username, @Nullable String password, String name, String provider,
							@Nullable Boolean isAdmin) throws IOException {
		CustomUserApi userApi = new CustomUserApi(getAdminGitLabApi());

		if (password == null) {
			// generate a random password
			password = RandomStringUtils.random(
				12, 0, GitLabServerManager.PWD_CHARS.length-1,
				false, false, GitLabServerManager.PWD_CHARS, new SecureRandom()
			);
		}

		if (isAdmin == null) {
			isAdmin = false;
		}

		User user = new User();
		user.setEmail(email);
		user.setUsername(username);
		user.setName(name);
		user.setIsAdmin(isAdmin);
		
		if (provider != null) {
			Identity identity = new Identity();
			identity.setExternUid(username);
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

	/**
	 * Creates a new remote user.
	 * <p>
	 * This action is performed as a GitLab admin.
	 *
	 * @param email the email address of the user to create
	 * @param username the username of the user to create
	 * @param password the password of the user to create. A random, 12 character password will be
	 *                 generated if none is supplied.
	 * @param name the name of the user to create
	 * @param isAdmin whether the user to create should be an admin or not. Defaults to false if not
	 *                supplied.
	 * @return the new user ID
	 * @throws IOException if something went wrong while creating the remote
	 *         user
	 */
	@Override
	public int createUser(String email, String username, @Nullable String password,
						   String name, @Nullable Boolean isAdmin)
			throws IOException {
		User user = this._createUser(email, username, password, name, null, isAdmin);
		return user.getId();
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
		CustomUserApi customUserApi = new CustomUserApi(this.adminGitLabApi);

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
	public ProjectReference findProjectReferenceById(String projectId) throws IOException {
		try {

			Group group = this.userGitLabApi.getGroupApi().getGroup(Objects.requireNonNull(projectId));
			return new ProjectReference(group.getPath(),group.getDescription(),"TODO");
		} catch (GitLabApiException e) {
			throw new IOException("failed to fetch project ", e);
		}
	}

	@Override
	public List<de.catma.user.User> getProjectMembers(String projectId) throws Exception {
		Group group = this.userGitLabApi.getGroupApi().getGroup(Objects.requireNonNull(projectId));
		return this.userGitLabApi.getGroupApi().getMembers(group.getId())
				.stream()
				.map(member -> new GitMember(member))
				.collect(Collectors.toList());
	}

	@Override
	public Pager<ProjectReference> getProjectReferences() throws IOException {
		
		GroupApi groupApi = this.userGitLabApi.getGroupApi();
		try {
			return new GitLabPager<>(
					groupApi.getGroups(30),//TODO: constant
					group -> new ProjectReference(
							group.getPath(), group.getDescription(), "TODO"));
		}
		catch (Exception e) {
			throw new IOException("Failed to load groups", e);
		}
	}
	
	@Override
	public String getProjectRootRepositoryUrl(ProjectReference projectReference) throws IOException {
		try {
			ProjectApi projectApi = this.userGitLabApi.getProjectApi();
			Project rootProject =projectApi.getProject(
				projectReference.getProjectId(), 
				GitProjectManager.getProjectRootRepositoryName(projectReference.getProjectId()));
			
			return getGitLabServerUrl(rootProject.getHttpUrlToRepo());
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to load Project's Root Repository Url", e);
		}
	}
	
	@Override
	public Set<String> getProjectRepositoryUrls(ProjectReference projectReference) throws IOException {
		try {
			GroupApi groupApi = this.userGitLabApi.getGroupApi();
			
			Group group = groupApi.getGroup(projectReference.getProjectId());
			return Collections.unmodifiableSet(groupApi
				.getProjects(group.getId())
				.stream()
				.map(project -> this.getGitLabServerUrl(project.getHttpUrlToRepo()))
				.collect(Collectors.toSet()));
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to load Group Repositories", e);
		}
	}

	@Override
	public String getUsername() {
		return gitLabUser.getUsername();
	}
	
	@Override
	public String getPassword() {
		return this.gitLabUserImpersonationToken;
	}

	public String getEmail() {
		return gitLabUser.getEmail();
	}
}
