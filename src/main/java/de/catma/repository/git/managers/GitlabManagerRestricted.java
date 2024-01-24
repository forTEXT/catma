package de.catma.repository.git.managers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.project.MergeRequestInfo;
import de.catma.project.ProjectReference;
import de.catma.project.ProjectsManager.ProjectMetadataSerializationField;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.GitGroup;
import de.catma.repository.git.GitLabUtils;
import de.catma.repository.git.GitMember;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.ui.events.ChangeUserAttributesEvent;
import de.catma.user.Member;
import de.catma.user.User;
import org.eclipse.jgit.lib.Constants;
import org.gitlab4j.api.Constants.IssueState;
import org.gitlab4j.api.Constants.MergeRequestState;
import org.gitlab4j.api.*;
import org.gitlab4j.api.models.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GitlabManagerRestricted extends GitlabManagerCommon implements RemoteGitManagerRestricted {
	public static final String CATMA_COMMENT_LABEL = "CATMA Comment";

	private final Logger logger = Logger.getLogger(GitlabManagerRestricted.class.getName());

	private final GitLabApi restrictedGitLabApi;
	private final Cache<String, List<?>> gitlabModelsCache;

	private GitUser user;

	public GitlabManagerRestricted(EventBus eventBus, String userImpersonationToken) throws IOException {
		this(eventBus, new GitLabApi(CATMAPropertyKey.GITLAB_SERVER_URL.getValue(), userImpersonationToken));
	}

	public GitlabManagerRestricted(EventBus eventBus, String username, String password) throws IOException {
		this(eventBus, oauth2Login(CATMAPropertyKey.GITLAB_SERVER_URL.getValue(), username, password));
	}

	private GitlabManagerRestricted(EventBus eventBus, GitLabApi api) throws IOException {
		this.restrictedGitLabApi = api;

		// cache rapid calls to getProjectReferences, like getProjectReferences().size() and getProjectReferences() from DashboardView
		this.gitlabModelsCache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build();

		try {
			this.user = new GitUser(this.restrictedGitLabApi.getUserApi().getCurrentUser());
		}
		catch (GitLabApiException e) {
			throw new IOException(e);
		}

		eventBus.register(this);
	}

	private static GitLabApi oauth2Login(String url, String username, String password) throws IOException {
		try {
			return GitLabApi.oauth2Login(url, username, password);
		}
		catch (GitLabApiException e) {
			throw new IOException(e);
		}
	}


	// event handlers
	@Subscribe
	public void handleChangeUserAttributes(ChangeUserAttributesEvent event){
		try {
			user = new GitUser(restrictedGitLabApi.getUserApi().getCurrentUser());
		}
		catch (GitLabApiException e) {
			logger.log(Level.WARNING, "Failed to fetch user from backend", e);
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


	// RemoteGitManagerCommon implementations
	@Override
	public boolean existsUserOrEmail(String usernameOrEmail) throws IOException {
		try {
			List<org.gitlab4j.api.models.User> users = restrictedGitLabApi.getUserApi().findUsers(usernameOrEmail);

			return users.stream()
					// exclude current user, exact matches only
					.filter(gitlabUser -> !gitlabUser.getId().equals(user.getUserId()))
					.anyMatch(gitlabUser -> gitlabUser.getUsername().equals(usernameOrEmail) || gitlabUser.getEmail().equals(usernameOrEmail));
		}
		catch (GitLabApiException e){
			throw new IOException("Failed to check whether user exists", e);
		}
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


	private ProjectReference getProjectReference(String namespace, String path, String description) throws IOException {
		try {
			JsonObject metaDataJson = JsonParser.parseString(description).getAsJsonObject();
			String catmaProjectName = metaDataJson.get(ProjectMetadataSerializationField.name.name()).getAsString();
			String catmaProjectDescription = metaDataJson.get(ProjectMetadataSerializationField.description.name()).getAsString();
			return new ProjectReference(path, namespace, catmaProjectName, catmaProjectDescription);
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

	@SuppressWarnings("unchecked")
	@Override
	public List<de.catma.user.Group> getGroups() throws IOException {
		try {
			return (List<de.catma.user.Group>) gitlabModelsCache.get("groups", () -> {
				List<de.catma.user.Group> result = new ArrayList<>();
				GroupApi groupApi = restrictedGitLabApi.getGroupApi();
				Pager<Group> groupPager = groupApi.getGroups(
						new GroupFilter()
								.withOrderBy(org.gitlab4j.api.Constants.GroupOrderBy.ID)
								.withSortOder(org.gitlab4j.api.Constants.SortOrder.DESC)
								.withTopLevelOnly(true),
						20);

				while (groupPager.hasNext()) {
					List<Group> groupPage = groupPager.next();

					for (Group group : groupPage) {
						if (group.getName().startsWith("CATMA_")) { // we reached legacy CATMA 6 groups and exit early with what we got so far
							return result;
						} else {
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
													project.getDescription()
											);
										} catch (IOException e) {
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
							result.add(new GitGroup(group, members, sharedProjects));
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
	public  de.catma.user.Group createGroup(String name) throws IOException {
		try {
			
			name = name.trim();
			String pathName = name.replaceAll("\s", "-").toLowerCase();
			
			GroupApi groupApi = restrictedGitLabApi.getGroupApi();

			GroupParams groupParams = new GroupParams().withName(name).withPath(pathName).withVisibility(Visibility.PRIVATE.name().toLowerCase());

			Group group = groupApi.createGroup(groupParams);

			return new GitGroup(group, Collections.emptySet(), Collections.emptyList());
		}
		catch (GitLabApiException e) {
			if (e.getMessage().contains("has already been taken")) {
				throw new IllegalArgumentException(String.format("The name '%1$s' has already been taken, please choose a different name!", name), e);
			}
			throw new IOException("Failed to create remote Git repository", e);
		}
	}


	@SuppressWarnings("unchecked")
	private List<ProjectReference> getProjectReferences(AccessLevel minAccessLevel) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();

			return (List<ProjectReference>) gitlabModelsCache.get(
					"projects",
					() -> projectApi.getProjects(new ProjectFilter().withMinAccessLevel(minAccessLevel).withMembership(true).withSimple(true))
							.stream()
							.filter(project -> !project.getNamespace().getName().startsWith("CATMA_")) // filter legacy projects
							.map(project -> {
								try {
									return getProjectReference(
											project.getNamespace().getPath(),
											project.getPath(),
											project.getDescription()
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
		return getProjectReferences(AccessLevel.forValue(RBACRole.ASSISTANT.getAccessLevel()));
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
			return projectApi.getMembers(projectReference.getFullPath())
					.stream()
					.map(GitMember::new)
					.collect(Collectors.toSet());
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
}
