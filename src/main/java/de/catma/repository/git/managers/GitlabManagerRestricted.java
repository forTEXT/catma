package de.catma.repository.git.managers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Namespace;
import org.gitlab4j.api.models.Project;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import de.catma.Pager;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.project.ProjectReference;
import de.catma.repository.git.CreateRepositoryResponse;
import de.catma.repository.git.GitMember;
import de.catma.repository.git.GitProjectManager;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.GitlabUtils;
import de.catma.repository.git.interfaces.IGitUserInformation;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.events.ChangeUserAttributeEvent;
import de.catma.user.User;
import elemental.json.Json;
import elemental.json.JsonException;
import elemental.json.JsonObject;

public class GitlabManagerRestricted implements IRemoteGitManagerRestricted, IGitUserInformation {

	private final GitLabApi restrictedGitLabApi;
	private final Logger logger = Logger.getLogger(this.getClass().getName());
	private GitUser user;

	@AssistedInject
	public GitlabManagerRestricted(EventBus eventBus, @Assisted("token") String userImpersonationToken) throws IOException {
		this(eventBus,new GitLabApi(RepositoryPropertyKey.GitLabServerUrl.getValue(), userImpersonationToken));
	}
	
	@AssistedInject
	public GitlabManagerRestricted(EventBus eventBus, @Assisted("username") String username, @Assisted("password") String password) throws IOException {
		this(eventBus, oauth2Login(RepositoryPropertyKey.GitLabServerUrl.getValue(), username, password));
	}

	private GitlabManagerRestricted(EventBus eventBus, GitLabApi api) throws IOException {
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
			return GitLabApi.oauth2Login(RepositoryPropertyKey.GitLabServerUrl.getValue(), username, password);
		} catch (GitLabApiException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public CreateRepositoryResponse createRepository(String name, @Nullable String path)
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
			String name, @Nullable String path, String groupPath)
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
	public String createGroup(String name, String path, @Nullable String description)
			throws IOException {
		GroupApi groupApi = restrictedGitLabApi.getGroupApi();

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
	public List<User> getProjectMembers(String projectId) throws Exception {
		Group group = restrictedGitLabApi.getGroupApi().getGroup(Objects.requireNonNull(projectId));
		return restrictedGitLabApi.getGroupApi().getMembers(group.getId())
				.stream()
				.map(member -> new GitMember(member))
				.collect(Collectors.toList());
	}

	@Override
	public Pager<ProjectReference> getProjectReferences() throws IOException {
		
		GroupApi groupApi = restrictedGitLabApi.getGroupApi();
		try {
			return new GitLabPager<>(
					groupApi.getGroups(30),
					group -> unmarshallProjectReference(
							group.getPath(),  group.getDescription()));
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
	public GitUser getGitUser() {
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

}
