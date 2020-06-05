package de.catma.repository.git.managers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import org.gitlab4j.api.models.GroupFilter;
import org.gitlab4j.api.models.Member;
import org.gitlab4j.api.models.Namespace;
import org.gitlab4j.api.models.Permissions;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectFilter;
import org.gitlab4j.api.models.Visibility;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import de.catma.backgroundservice.BackgroundService;
import de.catma.project.ForkStatus;
import de.catma.project.ProjectReference;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.CreateRepositoryResponse;
import de.catma.repository.git.GitMember;
import de.catma.repository.git.GitProjectManager;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.GitlabUtils;
import de.catma.repository.git.ResourceAlreadyExistsException;
import de.catma.repository.git.interfaces.IGitUserInformation;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.events.ChangeUserAttributeEvent;
import de.catma.user.User;
import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;

public class GitlabManagerRestricted extends GitlabManagerCommon implements IRemoteGitManagerRestricted, IGitUserInformation {

	private final GitLabApi restrictedGitLabApi;
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	private GitUser user;
	// Cache rapid calls to getProjectReferences, like getProjectReferences().size() and getProjectReferences() from DashboardView
	private final Cache<String, List<ProjectReference>> projectsCache = CacheBuilder.newBuilder()
		       .expireAfterWrite(5, TimeUnit.SECONDS)
		       .build();
	
	public GitlabManagerRestricted(EventBus eventBus, BackgroundService backgroundService, String userImpersonationToken) throws IOException {
		this(eventBus, backgroundService, new GitLabApi(CATMAPropertyKey.GitLabServerUrl.getValue(), userImpersonationToken));
	}
	
	public GitlabManagerRestricted(EventBus eventBus, BackgroundService backgroundService, String username, String password) throws IOException {
		this(eventBus, backgroundService, oauth2Login(CATMAPropertyKey.GitLabServerUrl.getValue(), username, password));
	}

	private GitlabManagerRestricted(EventBus eventBus, BackgroundService backgroundService, GitLabApi api) throws IOException {
		org.gitlab4j.api.models.User currentUser;
		try {
			currentUser = api.getUserApi().getCurrentUser();
			this.user = new GitUser(currentUser);
			this.restrictedGitLabApi = api;
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
			throw new IOException("Failed to delete remote group", e);
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
		return getProjectReferences(AccessLevel.forValue(RBACRole.GUEST.getAccessLevel()));
	}
	
	@Override
	public List<ProjectReference> getProjectReferences(RBACPermission withPermission) throws IOException {
		return getProjectReferences(AccessLevel.forValue(withPermission.getRoleRequired().getAccessLevel()));
	}
	
	private List<ProjectReference> getProjectReferences(AccessLevel minAccessLevel) throws IOException {
		GroupApi groupApi = new GroupApi(restrictedGitLabApi);
		try {
			return projectsCache.get("projects", () -> 
			 groupApi.getGroups(new GroupFilter().withMinAccessLevel(minAccessLevel))
			 	.stream()
			 	.map(group -> 
			 		unmarshallProjectReference(group.getPath(),  group.getDescription()))
				.collect(Collectors.toList()));
		}
		catch (Exception e) {
			throw new IOException("Failed to load groups", e);
		}
	}
	
	@Override
	public String getProjectRootRepositoryUrl(ProjectReference projectReference) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			Project rootProject = projectApi.getProject(
				projectReference.getProjectId(), 
				GitProjectManager.getProjectRootRepositoryName(projectReference.getProjectId()));
			
			return GitlabUtils.rewriteGitLabServerUrl(rootProject.getHttpUrlToRepo());
		}
		catch (GitLabApiException e) {
			throw new IOException(
				"Failed to load Project's Root Repository URL: " 
					+ GitProjectManager.getProjectRootRepositoryName(projectReference.getProjectId()), e);
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
			
			if(usernameOrEmail.isEmpty() || usernameOrEmail.length() < 3) {
				return Collections.emptyList();
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
				List<GitMember> allMembers = new ProjectApi(restrictedGitLabApi)
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

	public Map<String, RBACRole> getRolesPerResource(String projectId) throws IOException {
		try {
			Group group = restrictedGitLabApi.getGroupApi().getGroup(projectId);
			Map<String, AccessLevel> permMap = getResourcePermissions(group.getId());
			
			return permMap.entrySet()
					.stream()
					.collect(Collectors.toMap(
							Map.Entry::getKey,
							e -> RBACRole.forValue(e.getValue().value)));

		} catch (GitLabApiException e) {
			throw new IOException("Permission retrieval failed!",e);
		}
	}

	private Map<String, AccessLevel> getResourcePermissions(Integer groupId) throws GitLabApiException {

        Map<String, AccessLevel> resultMap = Maps.newHashMap();
        ProjectApi projectApi = new ProjectApi(restrictedGitLabApi);

        logger.info("Loading project permissions");

    	List<Project> resourceAndContainerProjects = 
    			projectApi.getProjects(new ProjectFilter().withMembership(true));

    	logger.info(String.format("Filtering %1$d resources on group #%2$d", resourceAndContainerProjects.size(), groupId));
        Set<Project> filteredOnGroupProjects = 
        	resourceAndContainerProjects
				.stream()
				.filter(p -> 
					p.getNamespace().getId().equals(groupId)) // the GitLab namespace/groupId
				.collect(Collectors.toSet());

        logger.info(String.format("Updating accesslevel registry for %1$d resources", filteredOnGroupProjects.size()));
		for (Project p : filteredOnGroupProjects) {
			Permissions permission = p.getPermissions();
			if (permission.getGroupAccess() != null) {
				resultMap.put(p.getName(), permission.getGroupAccess().getAccessLevel());
			}
			
    		if(permission.getProjectAccess() != null && 
    			(!resultMap.containsKey(p.getName()) 
        			|| resultMap.get(p.getName()).value.intValue() < permission.getProjectAccess().getAccessLevel().value.intValue())) {
        			
    			resultMap.put(p.getName(), permission.getProjectAccess().getAccessLevel());
    		}
		}

        return resultMap;
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
	
	@Override
	public ForkStatus forkResource(String resourceId, String sourceProjectId, String targetProjectId) throws IOException {
		try {
			Project sourceResourceProject = restrictedGitLabApi.getProjectApi().getProject(sourceProjectId, resourceId);
			Optional<Project> optionalTargetResource = restrictedGitLabApi.getProjectApi().getOptionalProject(targetProjectId, resourceId);
			if (optionalTargetResource.isPresent()) {
				return ForkStatus.resourceAlreadyExists();
			}
			
			restrictedGitLabApi.getProjectApi().forkProject(sourceResourceProject, targetProjectId);
			
			ProjectExtApi projectExtApi = new ProjectExtApi(restrictedGitLabApi);
			ProjectExt projectExt = projectExtApi.getProjectExt(targetProjectId, resourceId);

			String importStatus = projectExt.getImportStatus();
			
			int tries = 10;
			while (!importStatus.equals("finished") && tries > 0) {
				tries--;
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
				logger.info(
					String.format(
						"Trying to retrieve forked resource status for %1$s from group %2$s (try %3$d)",
						resourceId,
						targetProjectId,
						10-tries));
				importStatus = projectExt.getImportStatus();
			}
			
			if (!importStatus.equals("finished")) {
				logger.warning(String.format("Status is still '%1$s' and not 'finished'! Trying to continue anyway!", importStatus));
			}
			
			
			return ForkStatus.success();
		}
		catch (GitLabApiException e) {
			throw new IOException(
				String.format("Failed to fork resource %1$s from group %2$s into group %3$s", resourceId, sourceProjectId, targetProjectId), e);
		}
	}

}
