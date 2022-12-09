package de.catma.repository.git.managers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
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
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.GitLabUtils;
import de.catma.repository.git.GitMember;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.ui.events.ChangeUserAttributeEvent;
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
	private final Cache<String, List<ProjectReference>> projectsCache;

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
		this.projectsCache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.SECONDS).build();

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

	@Override
	public String createRepository(String name, String description) throws IOException {
		ProjectApi projectApi = restrictedGitLabApi.getProjectApi();

		try {
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
	public void deleteRepository(ProjectReference projectReference) throws IOException {
		ProjectApi projectApi = restrictedGitLabApi.getProjectApi();

		try {
			Project project= projectApi.getProject(projectReference.getNamespace(), projectReference.getProjectId());

			projectApi.deleteProject(project);
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to delete remote Git repository", e);
		}
	}
	
	@Override
	public void updateProject(String namespace, String projectId, String description) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			Project project= projectApi.getProject(namespace, projectId);
			project.setDescription(description);			
			projectApi.updateProject(project);
		}
		catch (GitLabApiException e) {
			throw new IOException(
				"Failed to update project description", e
			);
		}
	}


	@Deprecated
	public List<String> getGroupRepositoryNames(String path)
			throws IOException {
		GroupApi groupApi = restrictedGitLabApi.getGroupApi();

		try {
			List<Project> projects = groupApi.getProjects(path);
			return projects.stream().map(Project::getName).collect(Collectors.toList());
		}
		catch (GitLabApiException e) {
			throw new IOException(
				"Failed to get repository names for group", e
			);
		}
	}

	@Override
	public Set<de.catma.user.Member> getProjectMembers(ProjectReference projectReference) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			Project project = projectApi.getProject(projectReference.getNamespace(), projectReference.getProjectId());
			return projectApi.getMembers(project.getId())
					.stream()
					.map(GitMember::new)
					.collect(Collectors.toSet());
		}
		catch (GitLabApiException e) {
			throw new IOException("Failed to fetch project members", e);
		}
	}

	@Override
	public List<ProjectReference> getProjectReferences() throws IOException {
		return getProjectReferences(AccessLevel.forValue(RBACRole.ASSISTANT.getAccessLevel()));
	}

	private List<ProjectReference> getProjectReferences(AccessLevel minAccessLevel) throws IOException {
		ProjectApi projectApi = new ProjectApi(restrictedGitLabApi);

		try {
			return projectsCache.get("projects", () ->
					projectApi.getProjects(
							new ProjectFilter().withMinAccessLevel(minAccessLevel).withMembership(true)
					)
					.stream()
					.filter(project -> !project.getNamespace().getName().startsWith("CATMA_")) // filter legacy projects
					.map(project -> unmarshallProjectReference(
							project.getNamespace().getPath(),
							project.getPath(),
							project.getDescription()
					))
					.collect(Collectors.toList())
			);
		}
		catch (Exception e) {
			throw new IOException("Failed to load projects", e);
		}
	}

	@Override
	public String getProjectRepositoryUrl(ProjectReference projectReference) throws IOException {
		try {
			ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
			Project project = projectApi.getProject(
					projectReference.getNamespace(),
					projectReference.getProjectId());
			
			return GitLabUtils.rewriteGitLabServerUrl(project.getHttpUrlToRepo());
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format("Failed to get repository URL for project \"%s\" with ID %s", projectReference.getName(), projectReference.getProjectId()),
					e
			);
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
			throw new IOException("Failed to check whether user exists",e);
		}
	}
	
	@Override
	public List<User> findUser(String usernameOrEmail) throws IOException {
		try {
			List<org.gitlab4j.api.models.User> userPager;
			
			if(usernameOrEmail.isEmpty() || usernameOrEmail.length() < 3) {
				return Collections.emptyList();
			} else {
				userPager = this.restrictedGitLabApi.getUserApi()
						.findUsers(usernameOrEmail);
			}
			return userPager.stream()
			.filter(u -> ! user.getUserId().equals(u.getId()))
			.map(u -> new GitUser(u))
			.collect(Collectors.toList());
			
		} catch(GitLabApiException e){
			throw new IOException("Failed to search for users",e);
		}
	}
	
	@Override
	public void leaveProject(String namespace, String projectId) throws IOException {
		ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
		try {
			Project project = projectApi.getProject(namespace, projectId);
			Member member = projectApi.getMember(project.getId(), user.getUserId());
			if (member != null 
					&& member.getAccessLevel().value >= AccessLevel.GUEST.value 
					&& member.getAccessLevel().value < AccessLevel.OWNER.value) {
				projectApi.removeMember(project.getId(), user.getUserId());
			}
		} catch  (GitLabApiException ge){
			throw new IOException("Couldn't leave project",ge);
		}
	}
	
	private Map<String, AccessLevel> getResourcePermissions(Long groupId) throws GitLabApiException {

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

	private ProjectReference unmarshallProjectReference(
			String namespace, String path, String eventuallyMarshalledMetadata) {
		String name = "unknown";
		String description = "unknown";
		try {
			JsonObject metaDataJson = JsonParser.parseString(eventuallyMarshalledMetadata).getAsJsonObject();
			
			name = metaDataJson.get(ProjectMetadataSerializationField.name.name()).getAsString();
			description = metaDataJson.get(ProjectMetadataSerializationField.description.name()).getAsString();
		}
		catch (Exception e) {
			logger.log(
				Level.WARNING, 
				String.format(
					"Error retrieving project name or description for %1$s from %2$s",
					path, 
					eventuallyMarshalledMetadata), 
				e);
		}
		return new ProjectReference(path, namespace, name, description);
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
			logger.log(Level.WARNING, "Can't fetch user from backend", e);
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
	public void addComment(ProjectReference projectReference, Comment comment) throws IOException {
		String documentId = comment.getDocumentId();

		try {
			IssuesApi issuesApi = restrictedGitLabApi.getIssuesApi();

			String projectPath = String.format("%s/%s", projectReference.getNamespace(), projectReference.getProjectId());
			String title = comment.getBody().substring(0, Math.min(97, comment.getBody().length()));
			if (title.length() < comment.getBody().length()) {
				title += "...";
			}
			String description = new SerializationHelper<Comment>().serialize(comment);

			Issue issue = issuesApi.createIssue(
					projectPath,
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
							"Failed to create comment in project \"%s\" for document with ID %s",
							projectReference.getName(),
							documentId
					),
					e
			);
		}
	}

	@Override
	public List<Comment> getComments(ProjectReference projectReference, String documentId) throws IOException {
		try {
			IssuesApi issuesApi = restrictedGitLabApi.getIssuesApi();

			String projectPath = String.format("%s/%s", projectReference.getNamespace(), projectReference.getProjectId());
			Pager<Issue> issuePager = issuesApi.getIssues(
					projectPath,
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
							"Failed to retrieve comments for document with ID %s in project \"%s\"",
							documentId,
							projectReference.getName()
					),
					e
			);
		}
	}

	@Override
	public void removeComment(ProjectReference projectReference, Comment comment) throws IOException {
		String documentId = comment.getDocumentId();

		try {
			IssuesApi issuesApi = restrictedGitLabApi.getIssuesApi();

			String projectPath = String.format("%s/%s", projectReference.getNamespace(), projectReference.getProjectId());
			issuesApi.closeIssue(projectPath, comment.getIid());
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to delete comment with ID %1$s (issue IID %2$d) for document with ID %3$s in project \"%4$s\"",
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
	public void updateComment(ProjectReference projectReference, Comment comment) throws IOException {
		String documentId = comment.getDocumentId();

		try {
			IssuesApi issuesApi = restrictedGitLabApi.getIssuesApi();

			String projectPath = String.format("%s/%s", projectReference.getNamespace(), projectReference.getProjectId());
			String title = comment.getBody().substring(0, Math.min(97, comment.getBody().length()));
			if (title.length() < comment.getBody().length()) {
				title += "...";
			}
			String description = new SerializationHelper<Comment>().serialize(comment);

			issuesApi.updateIssue(
					projectPath, 
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
	public void addReply(ProjectReference projectReference, Comment comment, Reply reply) throws IOException {
		String documentId = comment.getDocumentId();

		try {
			NotesApi notesApi = restrictedGitLabApi.getNotesApi();

			String projectPath = String.format("%s/%s", projectReference.getNamespace(), projectReference.getProjectId());
			String noteBody = new SerializationHelper<Reply>().serialize(reply);
			Note note = notesApi.createIssueNote(projectPath, comment.getIid(), noteBody);

			reply.setId(note.getId());
			comment.addReply(reply);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to create reply to comment with ID %1$s (issue IID %2$d) for document with ID %3$s in project \"%4$s\"",
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
	public void updateReply(ProjectReference projectReference, Comment comment, Reply reply) throws IOException {
		String documentId = comment.getDocumentId();

		try {
			NotesApi notesApi = restrictedGitLabApi.getNotesApi();

			String projectPath = String.format("%s/%s", projectReference.getNamespace(), projectReference.getProjectId());
			String noteBody = new SerializationHelper<Reply>().serialize(reply);
			notesApi.updateIssueNote(projectPath, comment.getIid(), reply.getId(), noteBody);
		}
		catch (GitLabApiException e) {
			throw new IOException(
					String.format(
							"Failed to update reply with ID %1$s (note ID %2$d) on comment with ID %3$s (issue IID %4$d) " +
									"for document with ID %5$s in project \"%6$s\"",
							reply.getUuid(),
							reply.getId(),
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
	public void removeReply(ProjectReference projectReference, Comment comment, Reply reply) throws IOException {
		String documentId = comment.getDocumentId();

		try {
			NotesApi notesApi = restrictedGitLabApi.getNotesApi();

			String projectPath = String.format("%s/%s", projectReference.getNamespace(), projectReference.getProjectId());
			notesApi.deleteIssueNote(projectPath, comment.getIid(), reply.getId());

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
							documentId,
							projectReference.getName()
					),
					e
			);
		}
	}

	@Override
	public List<Reply> getCommentReplies(ProjectReference projectReference, Comment comment) throws IOException {
		String documentId = comment.getDocumentId();

		try {
			NotesApi notesApi = restrictedGitLabApi.getNotesApi();

			String projectPath = String.format("%s/%s", projectReference.getNamespace(), projectReference.getProjectId());
			List<Note> notes = notesApi.getIssueNotes(projectPath, comment.getIid());

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
							"Failed to retrieve replies to comment with ID %1$s (issue IID %2$d) for document with ID %3$s in project \"%4$s\"",
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
	public MergeRequestInfo getMergeRequest(ProjectReference projectReference, Long mergeRequestIid) throws IOException {
		String projectPath = projectReference.getNamespace() + "/" + projectReference.getProjectId();
		ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
		
		try {
			MergeRequestApi api = restrictedGitLabApi.getMergeRequestApi();
			Long glProjectId = projectApi.getProject(projectPath).getId();
			MergeRequest mr = api.getMergeRequest(glProjectId, mergeRequestIid);
			return new MergeRequestInfo(
					mr.getIid(), 
					mr.getTitle(), mr.getDescription(), 
					mr.getCreatedAt(), 
					mr.getState(), mr.getMergeStatus(),
					glProjectId);
			
		} catch (GitLabApiException e) {
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
	public List<MergeRequestInfo> getOpenMergeRequests(ProjectReference projectReference) throws IOException {
		String projectPath = projectReference.getNamespace() + "/" + projectReference.getProjectId();
		ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
		
		try {
			MergeRequestApi api = restrictedGitLabApi.getMergeRequestApi();
			MergeRequestFilter filter = new MergeRequestFilter();
			filter.setSourceBranch(user.getIdentifier());
			filter.setTargetBranch(Constants.MASTER);
			filter.setState(MergeRequestState.OPENED);
		
			Long glProjectId = projectApi.getProject(projectPath).getId();
			filter.setProjectId(glProjectId);
			List<MergeRequest> mergeRequests = api.getMergeRequests(filter);
			
			return mergeRequests.stream().map(
					mr -> new MergeRequestInfo(
							mr.getIid(), mr.getTitle(),
							mr.getDescription(), 
							mr.getCreatedAt(),
							mr.getState(),
							mr.getMergeStatus(),
							glProjectId))
					.sorted((mr1, mr2) -> mr1.getCreatedAt().compareTo(mr2.getCreatedAt()))
					.collect(Collectors.toList());
			
		} catch (GitLabApiException e) {
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
	public MergeRequestInfo createMergeRequest(ProjectReference projectReference) throws IOException {
		String projectPath = projectReference.getNamespace() + "/" + projectReference.getProjectId();
		ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
		
		try {
			MergeRequestApi api = restrictedGitLabApi.getMergeRequestApi();
			Long glProjectId = projectApi.getProject(projectPath).getId();
			
			MergeRequest mr = api.createMergeRequest(
					glProjectId, 
					user.getIdentifier(),
					Constants.MASTER, 
					String.format("Integration of latest changes by %s (%s)", user.getName(), user.getIdentifier()),
					String.format("Integration of latest changes by %s (%s)", user.getName(), user.getIdentifier()),
					null,
					null,
					null,
					null,
					false); // do not remove source branch
			
			return new MergeRequestInfo(
					mr.getIid(), mr.getTitle(),
					mr.getDescription(), 
					mr.getCreatedAt(),
					mr.getState(),
					mr.getMergeStatus(),
					glProjectId);
			
		} catch (GitLabApiException e) {
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
		MergeRequestApi api = restrictedGitLabApi.getMergeRequestApi();
		try {
			MergeRequest result = api.acceptMergeRequest(
					mergeRequestInfo.getGlProjectId(), mergeRequestInfo.getIid());
			return new MergeRequestInfo(
					result.getIid(), result.getTitle(),
					result.getDescription(), 
					result.getCreatedAt(),
					result.getState(),
					result.getMergeStatus(),
					mergeRequestInfo.getGlProjectId());
					
		} catch (GitLabApiException e) {
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
