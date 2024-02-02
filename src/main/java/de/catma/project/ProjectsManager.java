package de.catma.project;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import de.catma.rbac.RBACPermission;
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
	 * Get all the groups the current user has access to.
	 * 
	 * @return a {@link List} of {@link Group}s
	 * @throws IOException if an error occurs when getting the groups
	 */
	List<Group> getGroups() throws  IOException;

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
	 * @return the group
	 * @throws IOException if an error occurs when creating the group.
	 */
	Group createGroup(String name) throws IOException;

	/**
	 * Deletes the given group.
	 * @param group the group to delete
	 * @throws IOException if an error occurs when deleting the group.
	 */
	void deleteGroup(Group group) throws IOException;

	/**
	 * Updates the given group with the new name.
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

	Set<Member> getMembers(Group group) throws IOException;
}
