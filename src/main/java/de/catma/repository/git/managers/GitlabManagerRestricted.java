package de.catma.repository.git.managers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jgit.lib.Constants;
import org.gitlab4j.api.Constants.IssueState;
import org.gitlab4j.api.Constants.MergeRequestState;
import org.gitlab4j.api.EnhancedPager;
import org.gitlab4j.api.ExtendedCommitsApi;
import org.gitlab4j.api.ExtendedGroupFilter;
import org.gitlab4j.api.ExtendedProject;
import org.gitlab4j.api.ExtendedProjectApi;
import org.gitlab4j.api.ExtendedProjectFilter;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.IssuesApi;
import org.gitlab4j.api.MergeRequestApi;
import org.gitlab4j.api.NotesApi;
import org.gitlab4j.api.Pager;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.GroupFilter;
import org.gitlab4j.api.models.GroupParams;
import org.gitlab4j.api.models.GroupProjectsFilter;
import org.gitlab4j.api.models.ImportStatus.Status;
import org.gitlab4j.api.models.Issue;
import org.gitlab4j.api.models.IssueFilter;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.MergeRequestFilter;
import org.gitlab4j.api.models.Note;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectSharedGroup;
import org.gitlab4j.api.models.Visibility;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.project.BackendPager;
import de.catma.project.CommitInfo;
import de.catma.project.MergeRequestInfo;
import de.catma.project.ProjectReference;
import de.catma.project.ProjectsManager.ProjectMetadataSerializationField;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.GitGroup;
import de.catma.repository.git.GitLabUtils;
import de.catma.repository.git.GitMember;
import de.catma.repository.git.GitPager;
import de.catma.repository.git.GitSharedGroupMember;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.user.Member;
import de.catma.user.SharedGroup;
import de.catma.user.User;
import de.catma.util.IDGenerator;

public class GitlabManagerRestricted extends GitlabManagerCommon implements RemoteGitManagerRestricted {
	public static final String CATMA_COMMENT_LABEL = "CATMA Comment";

	private final Logger logger = Logger.getLogger(GitlabManagerRestricted.class.getName());

	private final GitLabApi restrictedGitLabApi;
	private final Cache<String, List<?>> gitlabModelsCache;

	private GitUser user;

	public GitlabManagerRestricted(String userImpersonationToken) throws IOException {
		this(new GitLabApi(CATMAPropertyKey.GITLAB_SERVER_URL.getValue(), userImpersonationToken));
	}

	public GitlabManagerRestricted(String username, String password) throws IOException {
		this(oauth2Login(CATMAPropertyKey.GITLAB_SERVER_URL.getValue(), username, password));
	}

	private GitlabManagerRestricted(GitLabApi api) throws IOException {
		this.restrictedGitLabApi = api;

		// cache rapid calls to getProjectReferences, like getProjectReferences().size() and getProjectReferences() from DashboardView
		this.gitlabModelsCache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();

		try {
			this.user = new GitUser(this.restrictedGitLabApi.getUserApi().getCurrentUser());
		}
		catch (GitLabApiException e) {
			throw new IOException(e);
		}
	}

	private static GitLabApi oauth2Login(String url, String username, String password) throws IOException {
		try {
			return GitLabApi.oauth2Login(url, username, password);
		}
		catch (GitLabApiException e) {
			throw new IOException(e);
		}
	}


	// GitlabManagerCommon implementations
	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public GitLabApi getGitLabApi() {
		return restrictedGitLabApi;
	}


	// GitUserInformationProvider implementations
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

	@Override
	public void refreshUserCredentials() throws IOException {
		try {
			logger.info("Attempting to refresh user credentials...");
			restrictedGitLabApi.oauth2RefreshAccessToken();
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to refresh user credentials", e);
		}
	}


	// RemoteGitManagerRestricted implementations
	@Override
	public void refreshUser() {
		try {
			user = new GitUser(restrictedGitLabApi.getUserApi().getCurrentUser());
		}
		catch (GitLabApiException e) {
			logger.log(Level.WARNING, "Failed to fetch user from backend", e);
		}
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public List<User> findUser(String usernameOrEmail) throws IOException {
		// only search with 3 or more characters
		if (usernameOrEmail == null || usernameOrEmail.length() < 3) {
			return Collections.emptyList();
		}

		try {
			List<org.gitlab4j.api.models.User> users = restrictedGitLabApi.getUserApi().findUsers(usernameOrEmail);
			return users.stream()
					.filter(gitlabUser -> !gitlabUser.getId().equals(user.getUserId())) // exclude current user
					.map(GitUser::new)
					.collect(Collectors.toList());
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to search for users", e);
		}
	}


	private ProjectReference getProjectReference(String namespace, String path, String description, ZonedDateTime createdAt,
			 ZonedDateTime lastActivityAt) throws IOException {
		try {
			JsonObject metaDataJson = JsonParser.parseString(description).getAsJsonObject();
			String catmaProjectName = metaDataJson.get(ProjectMetadataSerializationField.name.name()).getAsString();
			String catmaProjectDescription = metaDataJson.get(ProjectMetadataSerializationField.description.name()).getAsString();
			return new ProjectReference(
					path, namespace, catmaProjectName, catmaProjectDescription,
					createdAt, lastActivityAt);
		}
		catch (Exception e) {
			// while we could still return a ProjectReference with placeholder name and description, we probably want to investigate what
			// happened to this project before allowing the user to open it again (as it was likely modified externally)
			throw new IOException(
					String.format(
							"Failed to deserialize project metadata for GitLab project %s. The GitLab project description was: %s",
							namespace + "/" + path,
							description
					),
					e
			);
		}
	}

	@Override
	public List<de.catma.user.Group> getGroups(boolean forceRefetch) throws IOException {
		return getGroups(null, forceRefetch);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<de.catma.user.Group> getGroups(RBACRole minRole, boolean forceRefetch) throws IOException {
		try {
			
			String cacheKey = "groups" + minRole;
			if (forceRefetch) {
				gitlabModelsCache.invalidate(cacheKey);
			}
			
			return (List<de.catma.user.Group>) gitlabModelsCache.get(cacheKey, () -> {
				List<de.catma.user.Group> result = new ArrayList<>();
				GroupApi groupApi = restrictedGitLabApi.getGroupApi();
				GroupFilter groupFilter = new ExtendedGroupFilter()
						.withActive(true) // don't fetch groups that are archived or marked for deletion
						.withTopLevelOnly(true)
						.withOrderBy(org.gitlab4j.api.Constants.GroupOrderBy.ID)
						.withSortOder(org.gitlab4j.api.Constants.SortOrder.DESC);
				if (minRole != null) {
					groupFilter = groupFilter.withMinAccessLevel(AccessLevel.forValue(minRole.getAccessLevel()));
				}
				Pager<Group> groupPager = groupApi.getGroups(
						groupFilter,
						20);

				while (groupPager.hasNext()) {
					List<Group> groupPage = groupPager.next();

					for (Group group : groupPage) {
						if (group.getName().startsWith("CATMA_")) { // we reached legacy CATMA 6 project-groups and exit early with what we got so far
							return result;
						} else if (group.getPath().startsWith(IDGenerator.GROUP_ID_PREFIX)) { // valid user groups' paths start with G_
							try {
								Set<Member> members =
										groupApi.getMembers(group.getId())
												.stream()
												.map(GitMember::new)
												.collect(Collectors.toSet());

								List<ProjectReference> sharedProjects =
										groupApi.getProjects(
														group.getId(), new GroupProjectsFilter().withShared(true).withSimple(true)).stream().map(project -> {
													try {
														return getProjectReference(
																project.getNamespace().getPath(),
																project.getPath(),
																project.getDescription(),
																project.getCreatedAt() == null ? null : project.getCreatedAt().toInstant()
																		.atZone(ZoneId.systemDefault()),
																project.getLastActivityAt() == null ? null : project.getLastActivityAt().toInstant()
																		.atZone(ZoneId.systemDefault())
														);
													}
													catch (IOException e) {
														logger.log(
																Level.WARNING,
																String.format(
																		"Failed to get ProjectReference for GitLab project %s. The user won't be able to open this project.",
																		project.getNamespace().getPath() + "/" + project.getPath()
																),
																e
														);
														return null;
													}
												})
												.filter(Objects::nonNull)
												.toList();
								result.add(new GitGroup(group.getId(), group.getName(), group.getDescription(), members, sharedProjects));
							}
							catch (GitLabApiException e) {
								// ignore 404 errors when fetching a group's members or projects (groups that have been deleted since fetching the list)
								// rethrow all others
								if (e.getHttpStatus() != 404) {
									throw e;
								}
							}
						}
					}


				}
				return result;
			});
		}
		catch (Exception e) {
			throw new IOException("Failed to load groups", e);
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getOwnedGroupIds(boolean forceRefetch) throws IOException {
		try {
			String cacheKey = "ownedGroupIds"; 

			if (forceRefetch) {
				gitlabModelsCache.invalidate(cacheKey);
			}
			
			return (List<Long>) gitlabModelsCache.get(cacheKey, () -> {
				List<Long> result = new ArrayList<>();
				GroupApi groupApi = restrictedGitLabApi.getGroupApi();
				GroupFilter groupFilter = new ExtendedGroupFilter()
						.withActive(true) // don't fetch groups that are archived or marked for deletion
						.withOwned(true)
						.withTopLevelOnly(true)
						.withOrderBy(org.gitlab4j.api.Constants.GroupOrderBy.ID)
						.withSortOder(org.gitlab4j.api.Constants.SortOrder.DESC);

				Pager<Group> groupPager = groupApi.getGroups(
						groupFilter,
						20);

				while (groupPager.hasNext()) {
					List<Group> groupPage = groupPager.next();

					for (Group group : groupPage) {
						if (group.getName().startsWith("CATMA_")) { // we reached legacy CATMA 6 project-groups and exit early with what we got so far
							return result;
						} else if (group.getPath().startsWith(IDGenerator.GROUP_ID_PREFIX)) { // valid user groups' paths start with G_
							result.add(group.getId());
						}
					}
				}
				return result;
			});
		}
		catch (Exception e) {
			throw new IOException("Failed to load owned groupIds", e);
		}

	}
	
	@Override
	public de.catma.user.Group createGroup(String name, String path, String description) throws IOException {
		try {
			GroupApi groupApi = restrictedGitLabApi.getGroupApi();

			GroupParams groupParams = new GroupParams()
					.withName(name.trim())
					.withDescription(description.trim())
					.withPath(path)
					.withVisibility(Visibility.PRIVATE.name().toLowerCase());

			Group group = groupApi.createGroup(groupParams);

			return new GitGroup(group.getId(), group.getName(), group.getDescription(), Collections.emptySet(), Collections.emptyList());
		}
		catch (GitLabApiException e) {
			throw new IOException(String.format("Failed to create group %s", name), e);
		}
	}
	
	@Override
	public void deleteGroup(de.catma.user.Group group) throws IOException {
		try {
			GroupApi groupApi = restrictedGitLabApi.getGroupApi();
			groupApi.deleteGroup(group.getId());
		}
		catch (GitLabApiException e) {
			throw new IOException(String.format("Failed to delete group %s", group.getName()), e);
		}
	}


	@Override
	public de.catma.user.Group updateGroup(String name, String description, de.catma.user.Group group) throws IOException {
		try {
			GroupApi groupApi = restrictedGitLabApi.getGroupApi();
			GroupParams groupParams = new GroupParams().withName(name.trim()).withDescription(description.trim());
			Group updatedGroup = groupApi.updateGroup(group.getId(), groupParams);
			return new GitGroup(
					updatedGroup.getId(),
					updatedGroup.getName(),
					updatedGroup.getDescription(),
					group.getMembers().stream().collect(Collectors.toUnmodifiableSet()), 
					group.getSharedProjects().stream().collect(Collectors.toUnmodifiableList()));
		}
		catch (GitLabApiException e) {
			throw new IOException(String.format("Failed to update group %s", group.getName()), e);
		}
	}
	
	@Override
	public void leaveGroup(de.catma.user.Group group) throws IOException {
		unassignFromGroup(user, group);
	}
	
	@Override
	public void unassignFromGroup(RBACSubject subject, de.catma.user.Group group) throws IOException {
		try {
			GroupApi groupApi = restrictedGitLabApi.getGroupApi();
			org.gitlab4j.api.models.Member member = groupApi.getMember(group.getId(), subject.getUserId());
	
			if (member != null
					&& member.getAccessLevel().value >= AccessLevel.GUEST.value
					&& member.getAccessLevel().value < AccessLevel.OWNER.value
			) {
				groupApi.removeMember(group.getId(), member.getId());
			}
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to remove subject from group", e);
		}		
	}

	@SuppressWarnings("unchecked")
	private List<ProjectReference> getProjectReferences(AccessLevel minAccessLevel, Boolean owned, boolean forceRefresh) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			String cacheKey = "projects" + minAccessLevel + owned; 
			if (forceRefresh) {
				gitlabModelsCache.invalidate(cacheKey);
			}
			return (List<ProjectReference>) gitlabModelsCache.get(
					cacheKey,
					() -> projectApi.getProjects(
								new ExtendedProjectFilter()
								.withActive(true) // don't fetch projects that are archived or marked for deletion
								.withOwned(owned)
								.withMembership(true)
								.withMinAccessLevel(minAccessLevel)
								.withSimple(true))
							.stream()
							.filter(project -> !project.getNamespace().getName().startsWith("CATMA_")) // filter legacy projects
							.map(project -> {
								try {
									return getProjectReference(
											project.getNamespace().getPath(),
											project.getPath(),
											project.getDescription(),
											project.getCreatedAt() == null?null:project.getCreatedAt().toInstant()
												      .atZone(ZoneId.systemDefault()),
											project.getLastActivityAt() == null?null:project.getLastActivityAt().toInstant()
												      .atZone(ZoneId.systemDefault())
									);
								}
								catch (IOException e) {
									logger.log(
											Level.WARNING,
											String.format(
													"Failed to get ProjectReference for GitLab project %s. The user won't be able to open this project.",
													project.getNamespace().getPath() + "/" + project.getPath()
											),
											e
									);
									return null;
								}
							})
							.filter(Objects::nonNull)
							.collect(Collectors.toList())
			);
		}
		catch (Exception e) {
			throw new IOException("Failed to load projects", e);
		}
	}

	@Override
	public List<ProjectReference> getProjectReferences() throws IOException {
		return getProjectReferences(AccessLevel.forValue(RBACRole.ASSISTANT.getAccessLevel()), null, false);
	}
	
	@Override
	public ProjectReference getProjectReference(String namespace, String projectId) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			Project project = projectApi.getProject(namespace, projectId);
			return getProjectReference(
					project.getNamespace().getPath(),
					project.getPath(),
					project.getDescription(),
					project.getCreatedAt() == null?null:project.getCreatedAt().toInstant()
						      .atZone(ZoneId.systemDefault()),
					project.getLastActivityAt() == null?null:project.getLastActivityAt().toInstant()
						      .atZone(ZoneId.systemDefault())
			);
		}
		catch (GitLabApiException e) {
			throw new IOException(String.format("Failed to load project %s/%s", namespace, projectId), e);
		}

	}

	@Override
	public List<ProjectReference> getProjectReferences(boolean forceRefetch) throws IOException {
		return getProjectReferences(AccessLevel.forValue(RBACRole.ASSISTANT.getAccessLevel()), null, forceRefetch);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<String> getOwnedProjectIds(boolean forceRefetch) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			String cacheKey = "ownedProjectIds"; 

			if (forceRefetch) {
				gitlabModelsCache.invalidate(cacheKey);
			}

			return (List<String>) gitlabModelsCache.get(
					cacheKey,
					() -> projectApi.getProjects(
								new ExtendedProjectFilter()
								.withActive(true) // don't fetch projects that are archived or marked for deletion
								.withOwned(true)
								.withMinAccessLevel(AccessLevel.forValue(RBACRole.ASSISTANT.getAccessLevel()))
								.withSimple(true))
							.stream()
							.filter(project -> !project.getNamespace().getName().startsWith("CATMA_")) // filter legacy projects
							.map(Project::getPath)
							.filter(Objects::nonNull)
							.collect(Collectors.toList())
			);
		}
		catch (Exception e) {
			throw new IOException("Failed to load owned projectIds", e);
		}
	}
	
	
	@Override
	public String getProjectRepositoryUrl(ProjectReference projectReference) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			Project project = projectApi.getProject(projectReference.getFullPath());

			return GitLabUtils.rewriteGitLabServerUrl(project.getHttpUrlToRepo());
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to get repository URL for project \"%s\" with ID %s",
							projectReference.getName(),
							projectReference.getProjectId()
					),
					e
			);
		}
	}

	@Override
	public Set<Member> getProjectMembers(ProjectReference projectReference) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			// add members via direct membership
			Set<Member> members =  projectApi.getMembers(projectReference.getFullPath())
					.stream()
					.map(GitMember::new)
					.collect(Collectors.toCollection(() -> new HashSet<>()));
			
			// all members via group membership
			Project project = projectApi.getProject(projectReference.getFullPath());
			
			GroupApi groupApi = restrictedGitLabApi.getGroupApi();
			
			for (ProjectSharedGroup projectSharedGroup : project.getSharedWithGroups()) {

				List<org.gitlab4j.api.models.Member> groupMembers = groupApi.getMembers(projectSharedGroup.getGroupId());
				for (org.gitlab4j.api.models.Member groupMember : groupMembers) {
					members.add(
						new GitSharedGroupMember(
								groupMember, 
								new SharedGroup(projectSharedGroup.getGroupId(), projectSharedGroup.getGroupName(), RBACRole.forValue(projectSharedGroup.getGroupAccessLevel().value))));
				}
				
			}
			
			return members;
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to fetch project members", e);
		}
	}


	@Override
	public String createProject(String name, String description) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();

			Project project = new Project();
			project.setName(name);
			project.setDescription(description);
			project.setRemoveSourceBranchAfterMerge(false);
			project = projectApi.createProject(project);

			return GitLabUtils.rewriteGitLabServerUrl(project.getHttpUrlToRepo());
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to create remote Git repository", e);
		}
	}

	@Override
	public void updateProjectDescription(ProjectReference projectReference, String description) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			Project project = projectApi.getProject(projectReference.getFullPath());
			project.setDescription(description);
			projectApi.updateProject(project);
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to update project description", e);
		}
	}

	@Override
	public void leaveProject(ProjectReference projectReference) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			org.gitlab4j.api.models.Member member = projectApi.getMember(projectReference.getFullPath(), user.getUserId());

			if (member != null
					&& member.getAccessLevel().value >= AccessLevel.GUEST.value
					&& member.getAccessLevel().value < AccessLevel.OWNER.value
			) {
				projectApi.removeMember(projectReference.getFullPath(), user.getUserId());
			}
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to leave project", e);
		}
	}

	@Override
	public void deleteProject(ProjectReference projectReference) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			projectApi.deleteProject(projectReference.getFullPath());
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to delete remote Git repository", e);
		}
	}


	@Override
	public List<Comment> getComments(ProjectReference projectReference, String documentId) throws IOException {
		try {
			IssuesApi issuesApi = restrictedGitLabApi.getIssuesApi();

			Pager<Issue> issuePager = issuesApi.getIssues(
					projectReference.getFullPath(),
					new IssueFilter()
							.withLabels(Arrays.asList(CATMA_COMMENT_LABEL, documentId))
							.withState(IssueState.OPENED),
					100
			);

			List<Comment> comments = new ArrayList<>();

			for (Issue issue : issuePager.all()) {
				Comment comment;
				String issueDescription = issue.getDescription();

				try {
					comment = new SerializationHelper<Comment>().deserialize(issueDescription, Comment.class);
				}
				catch (Exception e) {
					logger.log(
							Level.SEVERE,
							String.format(
									"Failed to deserialize comment from issue with IID %1$d for document with ID %2$s in project \"%3$s\". " +
											"The issue description was: %4$s",
									issue.getIid(),
									documentId,
									projectReference.getName(),
									issueDescription
							),
							e
					);
					continue;
				}

				comment.setId(issue.getId());
				comment.setIid(issue.getIid());
				comment.setUserId(issue.getAuthor().getId());
				comment.setUsername(issue.getAuthor().getName()); // TODO: if we're using the public name it shouldn't be called 'username' on the Comment class
				comment.setReplyCount(issue.getUserNotesCount());
				
				// gson doesn't initialize transient fields, so we need to do it explicitly
				comment.setReplies(new ArrayList<>());
				
				comments.add(comment);
			}

			return comments;
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to fetch comments for document with ID %s in project \"%s\"",
							documentId,
							projectReference.getName()
					),
					e
			);
		}
	}

	@Override
	public void addComment(ProjectReference projectReference, Comment comment) throws IOException {
		String documentId = comment.getDocumentId();

		try {
			IssuesApi issuesApi = restrictedGitLabApi.getIssuesApi();

			String title = comment.getBody().substring(0, Math.min(97, comment.getBody().length()));
			if (title.length() < comment.getBody().length()) {
				title += "...";
			}
			String description = new SerializationHelper<Comment>().serialize(comment);

			Issue issue = issuesApi.createIssue(
					projectReference.getFullPath(),
					title,
					description,
					null,
					null,
					null,
					CATMA_COMMENT_LABEL + "," + documentId,
					null,
					null,
					null,
					null
			);

			comment.setId(issue.getId());
			comment.setIid(issue.getIid());
		}
		catch (GitLabApiException | IllegalArgumentException e) { // missing issue title throws IllegalArgumentException
			throw new IOException(
					String.format(
							"Failed to create comment for document with ID %s in project \"%s\"",
							documentId,
							projectReference.getName()
					),
					e
			);
		}
	}

	@Override
	public void updateComment(ProjectReference projectReference, Comment comment) throws IOException {
		String documentId = comment.getDocumentId();

		try {
			IssuesApi issuesApi = restrictedGitLabApi.getIssuesApi();

			String title = comment.getBody().substring(0, Math.min(97, comment.getBody().length()));
			if (title.length() < comment.getBody().length()) {
				title += "...";
			}
			String description = new SerializationHelper<Comment>().serialize(comment);

			issuesApi.updateIssue(
					projectReference.getFullPath(),
					comment.getIid(),
					title,
					description,
					null,
					null,
					null,
					CATMA_COMMENT_LABEL + "," + documentId,
					null,
					null,
					null
			);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to update comment with ID %1$s (issue IID %2$d) for document with ID %3$s in project \"%4$s\"",
							comment.getUuid(),
							comment.getIid(),
							documentId,
							projectReference.getName()
					),
					e
			);
		}
	}

	@Override
	public void removeComment(ProjectReference projectReference, Comment comment) throws IOException {
		try {
			IssuesApi issuesApi = restrictedGitLabApi.getIssuesApi();
			issuesApi.closeIssue(projectReference.getFullPath(), comment.getIid());
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to delete comment with ID %1$s (issue IID %2$d) for document with ID %3$s in project \"%4$s\"",
							comment.getUuid(),
							comment.getIid(),
							comment.getDocumentId(),
							projectReference.getName()
					),
					e
			);
		}
	}

	@Override
	public List<Reply> getCommentReplies(ProjectReference projectReference, Comment comment) throws IOException {
		try {
			NotesApi notesApi = restrictedGitLabApi.getNotesApi();
			List<Note> notes = notesApi.getIssueNotes(projectReference.getFullPath(), comment.getIid());

			List<Reply> replies = new ArrayList<>();

			for (Note note : notes.stream().filter(n -> !n.getSystem()).collect(Collectors.toList())) { // filter system notes
				Reply reply;
				String noteBody = note.getBody();

				try {
					reply = new SerializationHelper<Reply>().deserialize(noteBody, Reply.class);
				}
				catch (Exception e) {
					logger.log(
							Level.SEVERE,
							String.format(
									"Failed to deserialize reply from note with ID %1$d on comment with ID %2$s (issue IID %3$d) in project \"%4$s\". " +
											"The note body was: %5$s",
									note.getId(),
									comment.getUuid(),
									comment.getIid(),
									projectReference.getName(),
									noteBody
							),
							e
					);
					continue;
				}

				reply.setCommentUuid(comment.getUuid());
				reply.setId(note.getId());
				reply.setUserId(note.getAuthor().getId());
				reply.setUsername(note.getAuthor().getName()); // TODO: if we're using the public name it shouldn't be called 'username' on the Reply class

				replies.add(reply);
			}

			comment.setReplies(replies);

			return replies;
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to fetch replies to comment with ID %1$s (issue IID %2$d) for document with ID %3$s in project \"%4$s\"",
							comment.getUuid(),
							comment.getIid(),
							comment.getDocumentId(),
							projectReference.getName()
					),
					e
			);
		}
	}

	@Override
	public void addReply(ProjectReference projectReference, Comment comment, Reply reply) throws IOException {
		try {
			NotesApi notesApi = restrictedGitLabApi.getNotesApi();

			String noteBody = new SerializationHelper<Reply>().serialize(reply);
			Note note = notesApi.createIssueNote(projectReference.getFullPath(), comment.getIid(), noteBody);

			reply.setId(note.getId());
			comment.addReply(reply);
		}
		catch (GitLabApiException | IllegalArgumentException e) { // missing note body throws IllegalArgumentException
			throw new IOException(
					String.format(
							"Failed to create reply to comment with ID %1$s (issue IID %2$d) for document with ID %3$s in project \"%4$s\"",
							comment.getUuid(),
							comment.getIid(),
							comment.getDocumentId(),
							projectReference.getName()
					),
					e
			);
		}
	}

	@Override
	public void updateReply(ProjectReference projectReference, Comment comment, Reply reply) throws IOException {
		try {
			NotesApi notesApi = restrictedGitLabApi.getNotesApi();

			String noteBody = new SerializationHelper<Reply>().serialize(reply);
			notesApi.updateIssueNote(projectReference.getFullPath(), comment.getIid(), reply.getId(), noteBody);
		}
		catch (GitLabApiException | IllegalArgumentException e) { // missing note body throws IllegalArgumentException
			throw new IOException(
					String.format(
							"Failed to update reply with ID %1$s (note ID %2$d) on comment with ID %3$s (issue IID %4$d) " +
									"for document with ID %5$s in project \"%6$s\"",
							reply.getUuid(),
							reply.getId(),
							comment.getUuid(),
							comment.getIid(),
							comment.getDocumentId(),
							projectReference.getName()
					),
					e
			);
		}
	}

	@Override
	public void removeReply(ProjectReference projectReference, Comment comment, Reply reply) throws IOException {
		try {
			NotesApi notesApi = restrictedGitLabApi.getNotesApi();
			notesApi.deleteIssueNote(projectReference.getFullPath(), comment.getIid(), reply.getId());

			comment.removeReply(reply);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to delete reply with ID %1$s (note ID %2$d) from comment with ID %3$s (issue IID %4$d) " +
									"for document with ID %5$s in project \"%6$s\"",
							reply.getUuid(),
							reply.getId(),
							comment.getUuid(),
							comment.getIid(),
							comment.getDocumentId(),
							projectReference.getName()
					),
					e
			);
		}
	}


	@Override
	public List<MergeRequestInfo> getOpenMergeRequests(ProjectReference projectReference) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			Long gitlabProjectId = projectApi.getProject(projectReference.getFullPath()).getId();

			MergeRequestFilter filter = new MergeRequestFilter();
			filter.setProjectId(gitlabProjectId);
			filter.setSourceBranch(user.getIdentifier());
			filter.setTargetBranch(Constants.MASTER);
			filter.setState(MergeRequestState.OPENED);

			MergeRequestApi mergeRequestApi = restrictedGitLabApi.getMergeRequestApi();
			List<MergeRequest> mergeRequests = mergeRequestApi.getMergeRequests(filter);

			return mergeRequests.stream()
					.map(
							mr -> new MergeRequestInfo(
									mr.getIid(),
									mr.getTitle(),
									mr.getDescription(),
									mr.getCreatedAt(),
									mr.getState(),
									mr.getMergeStatus(),
									gitlabProjectId
							)
					)
					.sorted(Comparator.comparing(MergeRequestInfo::getCreatedAt))
					.collect(Collectors.toList());
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to retrieve open merge requests for project \"%s\" and user \"%s\"",
							projectReference.getName(),
							user.getIdentifier()
					),
					e
			);
		}
	}

	@Override
	public MergeRequestInfo getMergeRequest(ProjectReference projectReference, Long mergeRequestIid) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			Long gitlabProjectId = projectApi.getProject(projectReference.getFullPath()).getId();

			MergeRequestApi mergeRequestApi = restrictedGitLabApi.getMergeRequestApi();
			MergeRequest mergeRequest = mergeRequestApi.getMergeRequest(gitlabProjectId, mergeRequestIid);

			return new MergeRequestInfo(
					mergeRequest.getIid(),
					mergeRequest.getTitle(),
					mergeRequest.getDescription(),
					mergeRequest.getCreatedAt(),
					mergeRequest.getState(),
					mergeRequest.getMergeStatus(),
					gitlabProjectId
			);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to retrieve merge request with IID %d for project \"%s\"",
							mergeRequestIid,
							projectReference.getName()
					),
					e
			);
		}
	}

	@Override
	public MergeRequestInfo createMergeRequest(ProjectReference projectReference) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			Long gitlabProjectId = projectApi.getProject(projectReference.getFullPath()).getId();

			MergeRequestApi mergeRequestApi = restrictedGitLabApi.getMergeRequestApi();
			MergeRequest mergeRequest = mergeRequestApi.createMergeRequest(
					gitlabProjectId,
					user.getIdentifier(),
					Constants.MASTER,
					String.format("Integration of latest changes by %s (%s)", user.getName(), user.getIdentifier()),
					String.format("Integration of latest changes by %s (%s)", user.getName(), user.getIdentifier()),
					null,
					null,
					null,
					null,
					false // do not remove source branch
			);

			return new MergeRequestInfo(
					mergeRequest.getIid(),
					mergeRequest.getTitle(),
					mergeRequest.getDescription(),
					mergeRequest.getCreatedAt(),
					mergeRequest.getState(),
					mergeRequest.getMergeStatus(),
					gitlabProjectId
			);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to create merge request for project \"%s\" and user \"%s\"",
							projectReference.getName(),
							user.getIdentifier()
					),
					e
			);
		}
	}

	@Override
	public MergeRequestInfo mergeMergeRequest(MergeRequestInfo mergeRequestInfo) throws IOException {
		try {
			MergeRequestApi mergeRequestApi = restrictedGitLabApi.getMergeRequestApi();
			MergeRequest result = mergeRequestApi.acceptMergeRequest(
					mergeRequestInfo.getGlProjectId(),
					mergeRequestInfo.getIid()
			);

			return new MergeRequestInfo(
					result.getIid(),
					result.getTitle(),
					result.getDescription(),
					result.getCreatedAt(),
					result.getState(),
					result.getMergeStatus(),
					mergeRequestInfo.getGlProjectId()
			);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to merge merge request with IID %1$d for GitLab project ID %2$d and user \"%3$s\"",
							mergeRequestInfo.getIid(),
							mergeRequestInfo.getGlProjectId(),
							user.getIdentifier()
					),
					e
			);
		}
	}
	
	@Override
	public void forkProject(ProjectReference projectReference, String targetProjectId) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			Project sourceProject = projectApi.getProject(projectReference.getFullPath());
			
			
			projectApi.forkProject(
					sourceProject.getId(), 
					this.user.getIdentifier(), // we always fork into the namespace of the current user 
					targetProjectId, // path 
					targetProjectId); // name
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to fork remote Git repository", e);
		}
	}
	
	@Override
	public boolean isProjectImportFinished(ProjectReference projectReference) throws IOException {
		try {
			
			ExtendedProjectApi projectApi = new ExtendedProjectApi(restrictedGitLabApi);
			ExtendedProject project = projectApi.getExtendedProject(projectReference.getFullPath());
			Status status = project.getImportStatus();
			if (status.equals(Status.NONE) || status.equals(Status.FINISHED)) {
				return true;
			}
			
			if (status.equals(Status.FAILED)) {
				throw new IOException(String.format(
						"Forking the new project '%s' with ID '%s' failed with error message '%s'", 
						projectReference.getName(), projectReference.getProjectId(), project.getImportError()));
			}
			
			return false;
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to check import status for remote Git repository", e);
		}
		
	}
	
	@Override
	public BackendPager<CommitInfo> getCommits(ProjectReference projectReference, LocalDate after, LocalDate before, String branch, String author) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			Long gitlabProjectId = projectApi.getProject(projectReference.getFullPath()).getId();

			ExtendedCommitsApi commitsApi = new ExtendedCommitsApi(restrictedGitLabApi);
			
			EnhancedPager<Commit> commitsPager = commitsApi.getCommitsWithEnhancedPager(
					gitlabProjectId, 
					branch, 
					after==null?null:java.util.Date.from(
							after.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()),
					before==null?null:java.util.Date.from(
							before.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()),
					author,
					15
			);
			
			
			return new GitPager<Commit, CommitInfo>(commitsPager, commit -> new CommitInfo(commit.getId(), commit.getTitle(), commit.getMessage(), commit.getCommittedDate(), commit.getAuthorName()));
			
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to retrieve commits", e);
		}
		
	}
}
