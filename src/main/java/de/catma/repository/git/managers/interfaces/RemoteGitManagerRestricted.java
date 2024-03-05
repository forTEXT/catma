package de.catma.repository.git.managers.interfaces;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.project.BackendPager;
import de.catma.project.CommitInfo;
import de.catma.project.MergeRequestInfo;
import de.catma.project.ProjectReference;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.user.Group;
import de.catma.user.Member;
import de.catma.user.User;

/**
 * Provides restricted access (current user scope) to the remote Git server API
 */
public interface RemoteGitManagerRestricted extends RemoteGitManagerCommon, GitUserInformationProvider {
	/**
	 * Gets the user for the current session.
	 *
	 * @return a {@link User}
	 */
	User getUser();

	/**
	 * Searches for users based on username or email address.
	 *
	 * @param usernameOrEmail complete or partial username or email address to search for
	 * @return a {@link List} of matching {@link User}s
	 * @throws IOException if an error occurs when searching for users
	 */
	List<User> findUser(String usernameOrEmail) throws IOException;


	/**
	 * Gets all projects the current user has access to.
	 *
	 * @return a {@link List} of {@link ProjectReference}s
	 * @throws IOException if an error occurs when getting the projects
	 */
	List<ProjectReference> getProjectReferences() throws IOException;
	
	/**
	 * Gets all projects the current user has access to.
	 * 
	 * @param forceRefetch true->force a re-fetch from the underlying remote git manager (currently Gitlab) 
	 * @return a {@link List} of {@link ProjectReference}s
	 * @throws IOException if an error occurs when getting the projects
	 */
	List<ProjectReference> getProjectReferences(boolean forceRefetch) throws IOException;

	
	/**
	 * Get a list of the IDs of all projects owned by the current user.
	 * 
	 * @param forceRefetch true->force a re-fetch from the underlying remote git manager (currently Gitlab) 
	 *
	 * @return a {@link List} of {@link ProjectReference#getProjectId()}s
	 * @throws IOException if an error occurs when getting the projects
	 */	
	List<String> getOwnedProjectIds(boolean forceRefetch) throws IOException;
	
	/**
	 * Get all groups the current user is part of.
	 *
	 * @param forceRefetch true->force a re-fetch from the underlying remote git manager (currently Gitlab)
	 * @return a {@link List} of {@link de.catma.user.Group}s
	 * @throws IOException if an error occurs when getting the groups
	 */
	List<de.catma.user.Group> getGroups(boolean forceRefetch) throws IOException;
	
	/**
	 * Get all groups the current user is part of and having at least the given role. 
	 *
	 * @param forceRefetch true->force a re-fetch from the underlying remote git manager (currently Gitlab)
	 * @param minRole the minimum role the user must have in the result groups
	 * @return a {@link List} of {@link de.catma.user.Group}s
	 * @throws IOException if an error occurs when getting the groups
	 */
	List<de.catma.user.Group> getGroups(RBACRole minRole, boolean forceRefetch) throws IOException;

	
	/**
	 * Get a list of the IDs of all groups owned by the current user.
	 * 
	 * @param forceRefetch true->force a re-fetch from the underlying remote git manager (currently Gitlab) 
	 *
	 * @return a {@link List} of {@link Group#getId()}s
	 * @throws IOException if an error occurs when getting the projects
	 */	
	List<Long> getOwnedGroupIds(boolean forceRefetch) throws IOException;
	
	/**
	 * Creates a new group.
	 * @param name the name of the group
	 * @param path the path of the group
	 * @return the group
	 * @throws IOException if an error occurs when creating the group
	 */
	Group createGroup(String name, String path) throws IOException;

	/**
	 * Deletes a group
	 * @param group the group to delete
	 * @throws IOException if an error occurs when deleting the group
	 */
	void deleteGroup(Group group) throws IOException;	

	/**
	 * Updates the given group with the given new name.
	 * @param name the new name
	 * @param group the group to update
	 * @return the updated group
	 * @throws IOException if an error occurs when updating the group
	 */
	Group updateGroup(String name, Group group) throws IOException;

	/**
	 * Leave the given group.
	 * @param group the group to leave
	 * @throws IOException if an error occurs when leaving the group
	 */
	void leaveGroup(Group group) throws IOException;
	
	/**
	 * Unassign the given member subject from the given group.
	 * @param subject the member
	 * @param group the group to remove from
	 * @throws IOException if an error occurs when removing the subject from the group
	 */
	void unassignFromGroup(RBACSubject subject, Group group) throws IOException;

	
	/**
	 * Gets a project's repository URL.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project whose repository URL should be fetched
	 * @return the repository URL
	 * @throws IOException if an error occurs when getting the repository URL
	 */
	String getProjectRepositoryUrl(ProjectReference projectReference) throws IOException;

	/**
	 * Gets a set of all project members.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project whose members should be fetched
	 * @return a {@link Set} of {@link Member}s
	 * @throws IOException if an error occurs when getting the project members
	 */
	Set<Member> getProjectMembers(ProjectReference projectReference) throws IOException;


	/**
	 * Creates a new project in the current user's namespace.
	 *
	 * @param name the name of the project
	 * @param description the description of the project
	 * @return the project's repository URL
	 * @throws IOException if an error occurs when creating the project
	 */
	String createProject(String name, String description) throws IOException;

	/**
	 * Updates the description of a project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project whose description should be updated
	 * @param description the new description
	 * @throws IOException if an error occurs when updating the project description
	 */
	void updateProjectDescription(ProjectReference projectReference, String description) throws IOException;

	/**
	 * Removes the current user from a project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to remove the user from
	 * @throws IOException if an error occurs when removing the user from the project
	 */
	void leaveProject(ProjectReference projectReference) throws IOException;

	/**
	 * Deletes an existing project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to delete
	 * @throws IOException if an error occurs when deleting the project
	 */
	void deleteProject(ProjectReference projectReference) throws IOException;


	/**
	 * Gets the comments for a particular document in a project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to fetch comments from
	 * @param documentId the ID of the document to fetch comments for
	 * @return a {@link List} of {@link Comment}s
	 * @throws IOException if an error occurs when getting the comments
	 */
	List<Comment> getComments(ProjectReference projectReference, String documentId) throws IOException;

	/**
	 * Adds a comment to a project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to add the comment to
	 * @param comment the {@link Comment} to add
	 * @throws IOException if an error occurs when adding the comment
	 */
	void addComment(ProjectReference projectReference, Comment comment) throws IOException;

	/**
	 * Updates an existing comment in a project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to update the comment in
	 * @param comment the {@link Comment} to update
	 * @throws IOException if an error occurs when updating the comment
	 */
	void updateComment(ProjectReference projectReference, Comment comment) throws IOException;

	/**
	 * Removes a comment from a project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to remove the comment from
	 * @param comment the {@link Comment} to remove
	 * @throws IOException if an error occurs when removing the comment
	 */
	void removeComment(ProjectReference projectReference, Comment comment) throws IOException;

	/**
	 * Gets the replies for an existing comment.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to fetch comment replies from
	 * @param comment the {@link Comment} whose replies should be fetched
	 * @return a {@link List} of {@link Reply}
	 * @throws IOException if an error occurs when getting the replies
	 */
	List<Reply> getCommentReplies(ProjectReference projectReference, Comment comment) throws IOException;

	/**
	 * Adds a new reply to an existing comment.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to add the comment reply to
	 * @param comment the {@link Comment} being replied to
	 * @param reply the {@link Reply} to add
	 * @throws IOException if an error occurs when adding the reply
	 */
	void addReply(ProjectReference projectReference, Comment comment, Reply reply) throws IOException;

	/**
	 * Updates an existing comment reply.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to update the comment reply in
	 * @param comment the {@link Comment} whose reply should be updated
	 * @param reply the {@link Reply} to update
	 * @throws IOException if an error occurs when updating the comment reply
	 */
	void updateReply(ProjectReference projectReference, Comment comment, Reply reply) throws IOException;

	/**
	 * Removes a reply from a comment.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to remove the comment reply from
	 * @param comment the {@link Comment} to remove the reply from
	 * @param reply the {@link Reply} to remove
	 * @throws IOException if an error occurs when removing the reply
	 */
	void removeReply(ProjectReference projectReference, Comment comment, Reply reply) throws IOException;


	/**
	 * Gets the open merge requests for a project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project whose merge requests should be fetched
	 * @return a {@link List} of {@link MergeRequestInfo}s
	 * @throws IOException if an error occurs when getting the merge requests
	 */
	List<MergeRequestInfo> getOpenMergeRequests(ProjectReference projectReference) throws IOException;

	/**
	 * Gets a particular merge request for a project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to fetch the merge request from
	 * @param mergeRequestIid the project-internal ID of the merge request to fetch
	 * @return a {@link MergeRequestInfo}
	 * @throws IOException if an error occurs when fetching the merge request
	 */
	MergeRequestInfo getMergeRequest(ProjectReference projectReference, Long mergeRequestIid) throws IOException;

	/**
	 * Creates a new merge request for a project.
	 * <p>
	 * This is to merge the current user's branch into master.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to create the merge request in
	 * @return a {@link MergeRequestInfo} containing details of the new merge request
	 * @throws IOException if an error occurs when creating the merge request
	 */
	MergeRequestInfo createMergeRequest(ProjectReference projectReference) throws IOException;

	/**
	 * Merges an existing merge request for a project.
	 *
	 * @param mergeRequestInfo a {@link MergeRequestInfo} indicating the merge request to be merged
	 * @return a new {@link MergeRequestInfo} containing the result
	 * @throws IOException if an error occurs when merging the merge request
	 */
	MergeRequestInfo mergeMergeRequest(MergeRequestInfo mergeRequestInfo) throws IOException;

	/**
	 * Forks the given project by using the targetProjectId for gitlab-project-name and gitlab-project-path
	 * @param projectReference the project to fork
	 * @param targetProjectId the projectId of the new project
	 * @return the new project's repository URL
	 * @throws IOException if an error occurs when forking the project
	 */
	void forkProject(ProjectReference projectReference, String targetProjectId)
			throws IOException;

	/**
	 * Checks the import status of the given project.
	 * @param projectReference the project to check
	 * @return true if the project has been imported successfully else false
	 * @throws IOException if an error occurs during the check or if the import status is 'failed'.
	 */
	boolean isProjectImportFinished(ProjectReference projectReference) throws IOException;

	
	/**
	 * Gets commits for the given project and filter criteria.
	 * @param projectReference the project to get commits for
	 * @param after commits with timestamp after or equal this date
	 * @param before commits with timestamp before or equal this date
	 * @param branch the branch to look at
	 * @param author commits with this author
	 * @return a pager for the resulting commits
	 * @throws IOException if an error occurs during retrieval
	 */
	BackendPager<CommitInfo> getCommits(ProjectReference projectReference, LocalDate after, LocalDate before, String branch, String author)
			throws IOException;
}
