package de.catma.repository.git.interfaces;

import java.io.IOException;
import java.util.List;

import de.catma.Pager;
import de.catma.project.ProjectReference;
import de.catma.repository.git.CreateRepositoryResponse;
import de.catma.repository.git.GitUser;
import de.catma.ui.authentication.Credentials;
import de.catma.user.User;

/**
 * restriced API access in user scope
 * 
 * @author db
 *
 */
public interface IRemoteGitManagerRestricted extends IGitUserInformation {

	/**
	 * Creates a new remote repository with the <code>name</code> and <code>path</code> supplied.
	 *
	 * @param name the name of the repository to create
	 * @param path the path of the repository to create
	 * @return a {@link CreateRepositoryResponse} object containing the repository ID and HTTP URL
	 * @throws IOException if something went wrong while creating the remote
	 *         repository
	 */
	CreateRepositoryResponse createRepository(String name, String path) throws IOException;

	/**
	 * Creates a new remote repository with the <code>name</code> and <code>path</code> supplied
	 * within the group specified by the <code>groupId</code>.
	 *
	 * @param name the name of the repository to create
	 * @param path the path of the repository to create
	 * @param groupPath the path of the group within which to create the repository
	 * @return a {@link CreateRepositoryResponse} object containing the repository ID and HTTP URL
	 * @throws IOException if something went wrong while creating the remote
	 *         repository
	 */
	CreateRepositoryResponse createRepository(String name, String path, String groupPath) throws IOException;

	/**
	 * Deletes an existing remote repository identified by <code>repositoryId</code>.
	 *
	 * @param repositoryId the ID of the repository to delete
	 * @throws IOException if something went wrong while deleting the remote
	 *         repository
	 */
	void deleteRepository(int repositoryId) throws IOException;

	/**
	 * Creates a new remote group with the <code>name</code>, <code>path</code> and
	 * <code>description</code> supplied.
	 *
	 * @param name the name of the group to create
	 * @param path the path of the group to create
	 * @param description the description of the group to create
	 * @return the new group path
	 * @throws IOException if something went wrong while creating the remote
	 *         group
	 */
	String createGroup(String name, String path, String description) throws IOException;

	/**
	 * Gets the names of all repositories in the remote group identified by <code>path</code>.
	 *
	 * @param path the path of the group whose repository names you want to get
	 * @return a {@link List<String>} of names
	 * @throws IOException if something went wrong while fetching the repository
	 *         names of the remote group
	 */
	List<String> getGroupRepositoryNames(String path) throws IOException;

	/**
	 * Deletes an existing remote group identified by <code>path</code>.
	 * <p>
	 * NB: Also deletes any child repositories!
	 *
	 * @param path the path of the group to delete
	 * @throws IOException if something went wrong while deleting the remote
	 *         group
	 */
	void deleteGroup(String path) throws IOException;

	/**
	 * finds a Project by id
	 * @param projectId
	 * @return
	 * @throws IOException
	 */
	ProjectReference findProjectReferenceById(String projectId) throws IOException;

	/**
	 * fetches a list of all project members
	 * @param projectId
	 * @return
	 * @throws Exception
	 */
	List<User> getProjectMembers(String projectId) throws Exception;

	/**
	 * Get a Pager to a ProjectReferences
	 * @return
	 * @throws IOException
	 */
	Pager<ProjectReference> getProjectReferences() throws IOException;

	/**
	 * finds the root project and constructs the URL
	 * @param projectReference
	 * @return
	 * @throws IOException
	 */
	String getProjectRootRepositoryUrl(ProjectReference projectReference) throws IOException;

	/**
	 * 
	 * @return current GitUser
	 */
	GitUser getGitUser();

	/**
	 *  
	 * @return current credetials
	 */
	Credentials getCredentials();

}
