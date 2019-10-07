package de.catma.repository.git.managers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.Namespace;
import org.gitlab4j.api.models.Permissions;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectAccess;
import org.gitlab4j.api.models.Visibility;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import de.catma.backgroundservice.BackgroundService;
import de.catma.project.ProjectReference;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.CreateRepositoryResponse;
import de.catma.repository.git.GitMember;
import de.catma.repository.git.GitProjectManager;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.GitlabUtils;
import de.catma.repository.git.interfaces.IGitUserInformation;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.managers.gitlab4j_api_custom.CustomGroupApi;
import de.catma.repository.git.managers.gitlab4j_api_custom.CustomProjectApi;
import de.catma.ui.events.ChangeUserAttributeEvent;
import de.catma.user.User;
import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;

public class GitlabManagerRestricted extends GitlabManagerCommon implements IRemoteGitManagerRestricted, IGitUserInformation {

	private final GitLabApi restrictedGitLabApi;
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	private final BackgroundService backgroundService;
	private GitUser user;
	// Cache rapid calls to getProjectReferences, like getProjectReferences().size() and getProjectReferences() from DashboardView
	private final Cache<String, List<ProjectReference>> projectsCache = CacheBuilder.newBuilder()
		       .expireAfterWrite(5, TimeUnit.SECONDS)
		       .build();
	
	@AssistedInject
	public GitlabManagerRestricted(EventBus eventBus, BackgroundService backgroundService, @Assisted("token") String userImpersonationToken) throws IOException {
		this(eventBus, backgroundService, new GitLabApi(CATMAPropertyKey.GitLabServerUrl.getValue(), userImpersonationToken));
	}
	
	@AssistedInject
	public GitlabManagerRestricted(EventBus eventBus, BackgroundService backgroundService, @Assisted("username") String username, @Assisted("password") String password) throws IOException {
		this(eventBus, backgroundService, oauth2Login(CATMAPropertyKey.GitLabServerUrl.getValue(), username, password));
	}

	private GitlabManagerRestricted(EventBus eventBus, BackgroundService backgroundService, GitLabApi api) throws IOException {
		org.gitlab4j.api.models.User currentUser;
		try {
			currentUser = api.getUserApi().getCurrentUser();
			this.user = new GitUser(currentUser);
			this.restrictedGitLabApi = api;
			this.backgroundService = backgroundService;
			eventBus.register(this);
		} catch (GitLabApiException e) {
			throw new IOException(e);
		}
	}
	
	/**
	 * A wrapper to hide imports from outside world 
	 * @param url
	 * @param username
	 * @param password
	 * @return
	 * @throws IOException
	 */
	private static GitLabApi oauth2Login(String url, String username, String password) throws IOException {
		try {
			return GitLabApi.oauth2Login(CATMAPropertyKey.GitLabServerUrl.getValue(), username, password);
		} catch (GitLabApiException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public CreateRepositoryResponse createRepository(String name, String path)
			throws IOException {
		ProjectApi projectApi = this.restrictedGitLabApi.getProjectApi();

		Project project = new Project();
		project.setName(name);

		if (StringUtils.isNotEmpty(path)) {
			project.setPath(path);
		}

		try {
			project = projectApi.createProject(project);
			return new CreateRepositoryResponse(
				null, project.getId(),
				GitlabUtils.rewriteGitLabServerUrl(project.getHttpUrlToRepo())
			);
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to create remote Git repository", e);
		}
	}
	
	@Override
	public CreateRepositoryResponse createRepository(
			String name, String path, String groupPath)
			throws IOException {
		GroupApi groupApi = restrictedGitLabApi.getGroupApi();
		ProjectApi projectApi = restrictedGitLabApi.getProjectApi();

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
				GitlabUtils.rewriteGitLabServerUrl(project.getHttpUrlToRepo())
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
		ProjectApi projectApi = restrictedGitLabApi.getProjectApi();

		try {
			projectApi.deleteProject(repositoryId);
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to delete remote Git repository", e);
		}
	}
	

	@Override
	public String createGroup(String name, String path, String description)
			throws IOException {
		GroupApi groupApi = restrictedGitLabApi.getGroupApi();

		try {
			// none of the addGroup overloads accept a Group object parameter
			groupApi.addGroup(
				name, path, description,
				Visibility.PRIVATE,
				null, null, null
			);

			return path;
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to create remote group", e);
		}
	}
	
	@Override
	public void updateGroup(String name, String path, String description) throws IOException {
		try {
			GroupApi groupApi = restrictedGitLabApi.getGroupApi();
			groupApi.updateGroup(path, name, path, description, null, null, null, null);
		}
		catch (GitLabApiException e) {
			throw new IOException(
				"Failed to update name/description for group", e
			);
		}
	}

	@Override
	public List<String> getGroupRepositoryNames(String path)
			throws IOException {
		GroupApi groupApi = restrictedGitLabApi.getGroupApi();

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
	
	@Override
	public void deleteGroup(String path) throws IOException {
		GroupApi groupApi = restrictedGitLabApi.getGroupApi();

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
						Thread.sleep(150);
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
	
	@Override
	public ProjectReference findProjectReferenceById(String projectId) throws IOException {
		try {

			Group group = restrictedGitLabApi.getGroupApi().getGroup(Objects.requireNonNull(projectId));
			return unmarshallProjectReference(group.getPath(),group.getDescription());
		} catch (GitLabApiException e) {
			throw new IOException("failed to fetch project ", e);
		}
	}

	@Override
	public Set<de.catma.user.Member> getProjectMembers(String projectId) throws IOException {
		try {
			Group group = restrictedGitLabApi.getGroupApi().getGroup(Objects.requireNonNull(projectId));
			return restrictedGitLabApi.getGroupApi().getMembers(group.getId())
					.stream()
					.map(member -> new GitMember(member))
					.collect(Collectors.toSet());
		} catch (GitLabApiException e) {
			throw new IOException("group unknown",e);
		}
	}

	@Override
	public List<ProjectReference> getProjectReferences() throws IOException {
		CustomGroupApi groupApi = new CustomGroupApi(restrictedGitLabApi);
		
		try {
			return projectsCache.get("projects", () -> 
			 groupApi.getGroups().stream().map(
					group -> unmarshallProjectReference(
							group.getPath(),  group.getDescription()))
					.collect(Collectors.toList())
					);
		}
		catch (Exception e) {
			throw new IOException("Failed to load groups", e);
		}
	}
	
	@Override
	public String getProjectRootRepositoryUrl(ProjectReference projectReference) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			Project rootProject =projectApi.getProject(
				projectReference.getProjectId(), 
				GitProjectManager.getProjectRootRepositoryName(projectReference.getProjectId()));
			
			return GitlabUtils.rewriteGitLabServerUrl(rootProject.getHttpUrlToRepo());
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to load Project's Root Repository Url", e);
		}
	}
	
	@Override
	public boolean existsUserOrEmail(String usernameOrEmail) throws IOException {
		try {
			List<org.gitlab4j.api.models.User> userPager = this.restrictedGitLabApi.getUserApi().findUsers(usernameOrEmail);
			
			return userPager.stream()
			.filter(u -> usernameOrEmail.equals(u.getUsername()) || usernameOrEmail.equals(u.getEmail()))
			.filter(u -> user.getUserId() != u.getId())
			.count() > 0;
			
		} catch(GitLabApiException e){
			throw new IOException("failed to check for username",e);
		}
	}
	
	@Override
	public List<User> findUser(String usernameOrEmail, int offset, int limit) throws IOException {
		try {
			List<org.gitlab4j.api.models.User> userPager;
			
			if(usernameOrEmail.isEmpty()) {
				userPager = this.restrictedGitLabApi.getUserApi()
					.getUsers(offset, limit);
			} else {
				userPager = this.restrictedGitLabApi.getUserApi()
						.findUsers(usernameOrEmail,offset, limit);
			}
			return userPager.stream()
			.filter(u -> ! user.getUserId().equals(u.getId()))
			.map(u -> new GitUser(u))
			.collect(Collectors.toList());
			
		} catch(GitLabApiException e){
			throw new IOException("failed to check for username",e);
		}
	}
	
	@Override 
	public void leaveGroup(String path) throws IOException {
		GroupApi groupApi = restrictedGitLabApi.getGroupApi();
		
		try {
			Group group = groupApi.getGroup(path);
			Member member = groupApi.getMember(group.getId(), user.getUserId());
			if(member != null && 
					member.getAccessLevel().value >= AccessLevel.GUEST.value && 
					member.getAccessLevel().value < AccessLevel.OWNER.value 
					){
				groupApi.removeMember(group.getId(), user.getUserId());
			}
		} catch (GitLabApiException ge){
			throw new IOException("Couldn't leave group",ge);
		}
	}
	
	@Override
	public Set<de.catma.user.Member> getResourceMembers(String projectId, String resourceId) throws IOException {
		try {
			Project project = restrictedGitLabApi.getProjectApi().getProject(projectId, resourceId);
			if(project != null ){
				List<GitMember> allMembers = new CustomProjectApi(restrictedGitLabApi, backgroundService.getExecutorService())
						.getAllMembers(project.getId())
						.stream()
						.map(member -> new GitMember(member))
						.collect(Collectors.toList());
				
				Map<Integer,de.catma.user.Member> mergedList = new HashMap<>();
				
				for(de.catma.user.Member m : allMembers){
					if(! mergedList.containsKey(m.getUserId()) 
							|| mergedList.get(m.getUserId()).getRole().getAccessLevel() < m.getRole().getAccessLevel()){
						mergedList.put(m.getUserId(), m);
					}
				}
				return mergedList.values().stream().collect(Collectors.toSet());
				
			} else {
				throw new IOException("resource unknown");
			}	
		} catch (GitLabApiException e){
			throw new IOException("resource unknown");
		}
	}
	
	public Map<String, RBACRole> getRolesPerResource(ProjectReference projectReference) throws IOException {
		return getRolesPerResource(projectReference.getProjectId());
	}	
	
	public Map<String, RBACRole> getRolesPerResource(String projectId) throws IOException {
		try {
			Group group = restrictedGitLabApi.getGroupApi().getGroup(projectId);
			CustomProjectApi customProjectApi = new CustomProjectApi(restrictedGitLabApi,  backgroundService.getExecutorService());			
			Map<String, AccessLevel> permMap = customProjectApi.getResourcePermissions(group.getId());
			
			return permMap.entrySet()
					.stream()
					.collect(Collectors.toMap(
							Map.Entry::getKey,
							e -> RBACRole.forValue(e.getValue().value)));

		} catch (GitLabApiException e){
			throw new IOException("retrieving permissions failed",e);
		}
	}
	
//	public Map<String, RBACRole> getRolesPerProject(ProjectReference projectReference) throws IOException {
//		try {
//			Group group = restrictedGitLabApi.getGroupApi().getGroup(projectReference.getProjectId());
//			CustomProjectApi customProjectApi = new CustomProjectApi(restrictedGitLabApi,  backgroundService.getExecutorService());			
//			Map<String, AccessLevel> permMap = customProjectApi.getResourcePermissions(group.getId());
//			
//			return permMap.entrySet()
//					.stream()
//					.collect(Collectors.toMap(
//							Map.Entry::getKey,
//							e -> RBACRole.forValue(e.getValue().value)));
//
//		} catch (GitLabApiException e){
//			throw new IOException("retrieving permissions failed",e);
//		}
//	}
	
	private RBACRole getEffectiveRole(Permissions permissions) {
		ProjectAccess groupRole = permissions.getGroupAccess();
		ProjectAccess projectRole = permissions.getProjectAccess();
		
		return RBACRole.forValue(
				Math.max(groupRole.getAccessLevel().value, 
						projectRole.getAccessLevel().value));
	}
	/**
	 * @deprecated is this old?
	 * @param projectReference
	 * @return
	 * @throws IOException
	 */
	@Deprecated
	public Set<String> getProjectRepositoryUrls(ProjectReference projectReference) throws IOException {
		try {
			GroupApi groupApi = restrictedGitLabApi.getGroupApi();
			
			Group group = groupApi.getGroup(projectReference.getProjectId());
			return Collections.unmodifiableSet(groupApi
				.getProjects(group.getId())
				.stream()
				.map(project -> GitlabUtils.rewriteGitLabServerUrl(project.getHttpUrlToRepo()))
				.collect(Collectors.toSet()));
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to load Group Repositories", e);
		}
	}

	private ProjectReference unmarshallProjectReference(String path, String eventuallyMarshalledMetadata){
		try {
			JsonObject obj = Json.parse(eventuallyMarshalledMetadata);
			String name = obj.get("name").asString();
			String desc = obj.get("description").asString();
			return new ProjectReference(path, name, desc);
		} catch (JsonException e) {
			return new ProjectReference(path, eventuallyMarshalledMetadata, "nonexistent description");
		}
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getUsername() {
		return user.getIdentifier();
	}
	
	@Override
	public String getPassword() {
		return restrictedGitLabApi.getAuthToken();
	}

	@Override
	public String getEmail() {
		return user.getEmail();
	}
		
	@Subscribe
	public void handleChangeUserAttributes(ChangeUserAttributeEvent event){
		try {
			this.user = new GitUser(restrictedGitLabApi.getUserApi().getCurrentUser());
		} catch (GitLabApiException e) {
			logger.log(Level.WARNING, "can't fetch user from backend", e);
		}
	}	
	
	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public GitLabApi getGitLabApi() {
		return restrictedGitLabApi;
	}

}
