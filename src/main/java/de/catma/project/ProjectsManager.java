package de.catma.project;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.tag.TagManager;
import de.catma.user.Group;
import de.catma.user.Member;
import de.catma.user.User;

public interface ProjectsManager {
	enum ProjectMetadataSerializationField {
		name,
		description,
	}

	boolean hasPermission(RBACRole role, RBACPermission permission);

	RBACRole getRoleOnGroup(Group group) throws IOException;

	RBACRole getRoleOnProject(ProjectReference projectReference) throws IOException;

	Set<Member> getProjectMembers(ProjectReference projectReference) throws IOException;

	/**
	 * Gets the user for the current session.
	 *
	 * @return a {@link User}
	 */
	User getUser();

	/**
	 * Whether the current user has the given permission on the given project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to check
	 * @param permission the {@link RBACPermission} to check
	 * @return true if the current user has the given permission on the given project, otherwise false
	 */
	boolean isAuthorizedOnProject(ProjectReference projectReference, RBACPermission permission);

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
	 * @return a {@link List} of {@link ProjectReference#getProjectId()}s
	 * @throws IOException if an error occurs when getting the projects
	 */	
	List<String> getOwnedProjectIds(boolean forceRefetch) throws IOException;

	/**
	 * Get a list of the IDs of all groups owned by the current user.
	 *
	 * @param forceRefetch true->force a re-fetch from the underlying remote git manager (currently Gitlab) 
	 * @return a {@link List} of {@link Group#getId()}s
	 * @throws IOException if an error occurs when getting the projects
	 */	
	List<Long> getOwnedGroupIds(boolean forceRefetch) throws IOException;

	
	/**
	 * Get all the groups the current user has access to.
	 * 
	 * @param forceRefetch true->force a re-fetch from the underlying remote git manager (currently Gitlab) 
	 * @return a {@link List} of {@link Group}s
	 * @throws IOException if an error occurs when getting the groups
	 */
	List<Group> getGroups(boolean forceRefetch) throws  IOException;

	/**
	 * Get all the groups the current user has access to with at least the given role.
	 * 
	 * @param minRole the minimum role the user must have in the result groups
	 * @param forceRefetch true->force a re-fetch from the underlying remote git manager (currently Gitlab) 
	 * @return a {@link List} of {@link Group}s
	 * @throws IOException if an error occurs when getting the groups
	 */
	List<Group> getGroups(RBACRole minRole, boolean forceRefetch) throws  IOException;

	/**
	 * Opens an existing project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to be opened
	 * @param tagManager a {@link TagManager}
	 * @param openProjectListener an {@link OpenProjectListener}
	 */
	void openProject(ProjectReference projectReference, TagManager tagManager, OpenProjectListener openProjectListener);

	/**
	 * Creates a new project.
	 *
	 * @param name the name of the project to create
	 * @param description the description of the project to create
	 * @return a {@link ProjectReference} for the new project
	 * @throws IOException if an error occurs when creating the project
	 */
	ProjectReference createProject(String name, String description) throws IOException;

	/**
	 * Updates the metadata (name & description) of a project.
	 *
	 * @param projectReference a {@link ProjectReference} containing the new metadata
	 * @throws IOException if an error occurs when updating the project metadata
	 */
	void updateProjectMetadata(ProjectReference projectReference) throws IOException;

	/**
	 * Leaves (removes the user from) a project.
	 * <p>
	 * Automatically deletes the local copy of the project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to leave
	 * @throws IOException if an error occurs when leaving the project
	 */
	void leaveProject(ProjectReference projectReference) throws IOException;

	/**
	 * Deletes a project.
	 * <p>
	 * Automatically deletes the local & remote copies of the project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to delete
	 * @throws IOException if an error occurs when deleting the project
	 */
	void deleteProject(ProjectReference projectReference) throws IOException;

	/**
	 * Creates a new group.
	 * 
	 * @param name the name of the group
	 * @param description the description of the group
	 * @return the group
	 * @throws IOException if an error occurs when creating the group.
	 */
	Group createGroup(String name, String description) throws IOException;

	/**
	 * Deletes the given group.
	 * @param group the group to delete
	 * @throws IOException if an error occurs when deleting the group.
	 */
	void deleteGroup(Group group) throws IOException;

	/**
	 * Updates the given group with the new name and description.
	 * @param name the new name
	 * @param description the new description
	 * @param group the group to update
	 * @return the updated group
	 * @throws IOException if an error occurs when updating the group
	 */
	Group updateGroup(String name, String description, Group group) throws IOException;

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
	 * Searches for users amongst all available users.
	 *
	 * @param usernameOrEmail the partial or complete username or email address to search for
	 * @return a {@link List} of {@link User}s
	 * @throws IOException if an error occurs when searching
	 */
	List<User> findUser(String usernameOrEmail) throws IOException;

	/**
	 * Forks the given project by using the given name and description for the new project.
	 * @param projectReference the source project to be forked
	 * @param name the name of the new project
	 * @param description the description of the new project
	 * @return the new project, make sure to check the import status before using it (e. g. cloning)
	 * @throws IOException if an error occurs when forking
	 */
	ProjectReference forkProject(ProjectReference projectReference, String name, String description) throws IOException;

	/**
	 * Checks the import status of the given project.
	 * @param projectReference the project to check
	 * @return true if the project has been imported successfully else false
	 * @throws IOException if an error occurs during the check or if the import status is 'failed'.
	 */
	boolean isProjectImportFinished(ProjectReference projectReference) throws IOException;

	/**
	 * Updates role and/or expiration date of a member of a group.
	 * @param userId the ID of the member of the group
	 * @param groupId the ID of the group
	 * @param role the new role
	 * @param expiresAt new expiration date
	 * @throws IOException
	 */
	void updateAssignmentOnGroup(Long userId, Long groupId, RBACRole role, LocalDate expiresAt) throws IOException;
}
