package de.catma.repository.git.managers.interfaces;

import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.project.MergeRequestInfo;
import de.catma.project.ProjectReference;
import de.catma.user.Member;
import de.catma.user.User;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Provides restricted access (current user scope) to the remote Git server API
 */
public interface RemoteGitManagerRestricted extends RemoteGitManagerCommon, GitUserInformationProvider {
	/**
	 * Creates a new remote repository in the current user's namespace.
	 *
	 * @param name the name of the repository
	 * @param description the description of the repository
	 * @return the repository URL
	 * @throws IOException if an error occurs when creating the remote repository
	 */
	String createRepository(String name, String description) throws IOException;

	/**
	 * Deletes an existing remote repository.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the repository to delete
	 * @throws IOException if an error occurs when deleting the remote repository
	 */
	void deleteRepository(ProjectReference projectReference) throws IOException;

	/**
	 * Gets a set of all project members.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project whose members should be fetched
	 * @return a {@link Set} of {@link Member}s
	 * @throws IOException if an error occurs when getting the project members
	 */
	Set<Member> getProjectMembers(ProjectReference projectReference) throws IOException;

	/**
	 * Gets all projects the current user has access to.
	 *
	 * @return a {@link List} of {@link ProjectReference}s
	 * @throws IOException if an error occurs when getting the projects
	 */
	List<ProjectReference> getProjectReferences() throws IOException;

	/**
	 * Gets a project's repository URL.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project whose repository URL should be fetched
	 * @return the repository URL
	 * @throws IOException if an error occurs when getting the repository URL
	 */
	String getProjectRepositoryUrl(ProjectReference projectReference) throws IOException;

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
	 * Removes the current user from a project.
	 *
	 * @param namespace the namespace of the project repository
	 * @param projectId the ID of the project repository
	 * @throws IOException if an error occurs when removing the user from the project
	 */
	void leaveProject(String namespace, String projectId) throws IOException;

	/**
	 * Updates the description of a project.
	 *
	 * @param namespace the namespace of the project repository
	 * @param projectId the ID of the project repository
	 * @param description the new description
	 * @throws IOException if an error occurs when updating the project description
	 */
	void updateProject(String namespace, String projectId, String description) throws IOException;

	/**
	 * Adds a comment to a project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to add the comment to
	 * @param comment the {@link Comment} to add
	 * @throws IOException if an error occurs when adding the comment
	 */
	void addComment(ProjectReference projectReference, Comment comment) throws IOException;

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
	 * Removes a comment from a project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to remove the comment from
	 * @param comment the {@link Comment} to remove
	 * @throws IOException if an error occurs when removing the comment
	 */
	void removeComment(ProjectReference projectReference, Comment comment) throws IOException;

	/**
	 * Updates an existing comment in a project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to update the comment in
	 * @param comment the {@link Comment} to update
	 * @throws IOException if an error occurs when updating the comment
	 */
	void updateComment(ProjectReference projectReference, Comment comment) throws IOException;

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
	 * Gets the replies for an existing comment.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to fetch comment replies from
	 * @param comment the {@link Comment} whose replies should be fetched
	 * @return a {@link List} of {@link Reply}
	 * @throws IOException if an error occurs when getting the replies
	 */
	List<Reply> getCommentReplies(ProjectReference projectReference, Comment comment) throws IOException;

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
	 * Updates an existing comment reply.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to update the comment reply in
	 * @param comment the {@link Comment} whose reply should be updated
	 * @param reply the {@link Reply} to update
	 * @throws IOException if an error occurs when updating the comment reply
	 */
	void updateReply(ProjectReference projectReference, Comment comment, Reply reply) throws IOException;

	/**
	 * Gets the open merge requests for a project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project whose merge requests should be fetched
	 * @return a {@link List} of {@link MergeRequestInfo}s
	 * @throws IOException if an error occurs when getting the merge requests
	 */
	List<MergeRequestInfo> getOpenMergeRequests(ProjectReference projectReference) throws IOException;

	/**
	 * Merges an existing merge request for a project.
	 *
	 * @param mergeRequestInfo a {@link MergeRequestInfo} indicating the merge request to be merged
	 * @return a new {@link MergeRequestInfo} containing the result
	 * @throws IOException if an error occurs when merging the merge request
	 */
	MergeRequestInfo mergeMergeRequest(MergeRequestInfo mergeRequestInfo) throws IOException;

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
	 * Gets a particular merge request for a project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to fetch the merge request from
	 * @param mergeRequestIid the project-internal ID of the merge request to fetch
	 * @return a {@link MergeRequestInfo}
	 * @throws IOException if an error occurs when fetching the merge request
	 */
	MergeRequestInfo getMergeRequest(ProjectReference projectReference, Long mergeRequestIid) throws IOException;
}
