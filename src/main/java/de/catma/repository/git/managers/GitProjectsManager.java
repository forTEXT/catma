package de.catma.repository.git.managers;

import com.google.common.eventbus.EventBus;
import com.google.gson.JsonObject;
import de.catma.backgroundservice.BackgroundService;
import de.catma.project.OpenProjectListener;
import de.catma.project.Project;
import de.catma.project.ProjectReference;
import de.catma.project.ProjectsManager;
import de.catma.rbac.RBACPermission;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.GraphWorktreeProject;
import de.catma.repository.git.graph.interfaces.GraphProjectDeletionHandler;
import de.catma.repository.git.managers.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.managers.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.managers.interfaces.IRemoteGitManagerRestricted.GroupSerializationField;
import de.catma.tag.TagManager;
import de.catma.user.User;
import de.catma.util.IDGenerator;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class GitProjectsManager implements ProjectsManager {
	private final String gitBasedRepositoryBasePath;
	private final IRemoteGitManagerRestricted remoteGitServerManager;
	private final GraphProjectDeletionHandler graphProjectDeletionHandler;
	private final BackgroundService backgroundService;
	private final EventBus eventBus;

	private final User user;
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final CredentialsProvider credentialsProvider;
	private final IDGenerator idGenerator;

	public GitProjectsManager(
			String gitBasedRepositoryBasePath,
			IRemoteGitManagerRestricted remoteGitServerManager,
			GraphProjectDeletionHandler graphProjectDeletionHandler,
			BackgroundService backgroundService,
			EventBus eventBus
	) {
		this.gitBasedRepositoryBasePath = gitBasedRepositoryBasePath;
		this.remoteGitServerManager = remoteGitServerManager;
		this.graphProjectDeletionHandler = graphProjectDeletionHandler;
		this.backgroundService = backgroundService;
		this.eventBus = eventBus;

		this.user = remoteGitServerManager.getUser();
		this.localGitRepositoryManager = new JGitRepoManager(this.gitBasedRepositoryBasePath, this.user);
		this.credentialsProvider = new UsernamePasswordCredentialsProvider("oauth2", remoteGitServerManager.getPassword());
		this.idGenerator = new IDGenerator();
	}

	/**
	 * Creates a new project.
	 *
	 * @param name the name of the project to create
	 * @param description the description of the project to create
	 * @return a {@link ProjectReference} for the new project
	 * @throws IOException if an error occurs when creating the project
	 */
	@Override
	public ProjectReference createProject(String name, String description) throws IOException {
		String serializedProjectMetadata = serializeProjectMetadata(name, description);

		// note restrictions on project path: https://docs.gitlab.com/ee/api/projects.html#create-project
		String cleanedName = name.trim()
				.replaceAll("[\\p{Punct}\\p{Space}]", "_") // replace punctuation and whitespace characters with underscore ( _ )
				.replaceAll("_+", "_") // collapse multiple consecutive underscores into one
				.replaceAll("[^\\p{Alnum}_]", "x") // replace any remaining non-alphanumeric characters with x (excluding underscore)
				.replaceAll("^_|_$", ""); // strip any leading or trailing underscore
		String projectId = String.format("%s_%s", idGenerator.generate(), cleanedName);

		try (ILocalGitRepositoryManager localGitRepoManager = localGitRepositoryManager) {
			// create the remote repository
			CreateRepositoryResponse response = remoteGitServerManager.createRepository(
					projectId, serializedProjectMetadata
			);

			// clone the repository locally
			localGitRepoManager.clone(
					user.getIdentifier(),
					projectId,
					response.repositoryHttpUrl,
					credentialsProvider
			);

			String initialCommit = String.format("Created project \"%s\"", name);
			localGitRepoManager.commit(
					initialCommit,
					remoteGitServerManager.getUsername(),
					remoteGitServerManager.getEmail(), true);

			localGitRepoManager.pushMaster(credentialsProvider);

			localGitRepoManager.checkout(user.getIdentifier(), true);

			// create remote user specific branch
			localGitRepoManager.push(credentialsProvider);
		}

		return new ProjectReference(projectId, user.getIdentifier(), name, description);
	}

	/**
	 * Opens an existing project.
	 *
	 * @param projectReference a {@link ProjectReference} indicating which project should be opened
	 * @param tagManager a {@link TagManager}
	 * @param openProjectListener an {@link OpenProjectListener}
	 */
	@Override
	public void openProject(
			ProjectReference projectReference,
			TagManager tagManager,
			OpenProjectListener openProjectListener
	) {
		try {
			cloneLocallyIfNotExists(projectReference, openProjectListener);

			Project project = new GraphWorktreeProject(
					user,
					new GitProjectHandler(
							user,
							projectReference,
							Paths.get(new File(gitBasedRepositoryBasePath).toURI())
									.resolve(user.getIdentifier())
									.resolve(projectReference.getNamespace())
									.resolve(projectReference.getProjectId())
									.toFile(),
							localGitRepositoryManager,
							remoteGitServerManager
					),
					projectReference,
					tagManager,
					backgroundService,
					eventBus
			);
			project.open(openProjectListener);
		}
		catch (Exception e) {
			openProjectListener.failure(e);
		}
	}

	/**
	 * Updates the metadata (name & description) of a project.
	 *
	 * @param projectReference a {@link ProjectReference} containing the new metadata
	 * @throws IOException if an error occurs when updating the project metadata
	 */
	@Override
	public void updateProjectMetadata(ProjectReference projectReference) throws IOException {
		String serializedProjectMetadata = serializeProjectMetadata(
				projectReference.getName(), projectReference.getDescription()
		);
		remoteGitServerManager.updateProject(
				projectReference.getNamespace(),
				projectReference.getProjectId(),
				serializedProjectMetadata
		);
	}

	/**
	 * Leaves (removes the user from) a project.
	 * <p>
	 * Automatically deletes the local repository.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to leave
	 * @throws IOException if an error occurs when leaving the project
	 */
	@Override
	public void leaveProject(ProjectReference projectReference) throws IOException {
		// TODO: consider checking for uncommitted/unpushed changes first
		FileUtils.deleteDirectory(new File(
				localGitRepositoryManager.getRepositoryBasePath(),
				projectReference.getProjectId()
		));

		remoteGitServerManager.leaveProject(
				projectReference.getNamespace(), projectReference.getProjectId()
		);

		try {
			graphProjectDeletionHandler.deleteProject(projectReference);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * Deletes a project.
	 * <p>
	 * Automatically deletes the local & remote repositories.
	 *
	 * @param projectReference a {@link ProjectReference} indicating the project to delete
	 * @throws IOException if an error occurs when deleting the project
	 */
	@Override
	public void deleteProject(ProjectReference projectReference) throws IOException {
		Path projectDir = Paths.get(
				localGitRepositoryManager.getRepositoryBasePath().getAbsolutePath(),
				projectReference.getNamespace(),
				projectReference.getProjectId()
		);

		// shouldn't be necessary, but at least on Windows some Git objects didn't have write permissions
		// during testing and prevented project deletion
		// TODO: this was added before the explicit repository close call was added in JGitRepoManager.close
		//       and can potentially be removed now
		Files.walkFileTree(
				projectDir,
				new SimpleFileVisitor<Path>() {
					public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
						filePath.toFile().setWritable(true, true);
						return FileVisitResult.CONTINUE;
					}
				}
		);

		FileUtils.deleteDirectory(
			Paths.get(
				localGitRepositoryManager.getRepositoryBasePath().getAbsolutePath(),
				projectReference.getNamespace(),
				projectReference.getProjectId()
			).toFile()
		);

		remoteGitServerManager.deleteRepository(projectReference);

		try {
			graphProjectDeletionHandler.deleteProject(projectReference);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public List<ProjectReference> getProjectReferences() throws IOException {
		return remoteGitServerManager.getProjectReferences();
	}

	@Override
	public List<ProjectReference> getProjectReferences(RBACPermission withPermission) throws IOException {
		return remoteGitServerManager.getProjectReferences(withPermission);
	}

	@Override
	public boolean isAuthorizedOnProject(ProjectReference projectReference, RBACPermission permission) {
		return remoteGitServerManager.isAuthorizedOnProject(user, permission, projectReference);
	}

	private void cloneLocallyIfNotExists(ProjectReference projectReference, OpenProjectListener openProjectListener) throws IOException {
		if (!Paths.get(new File(gitBasedRepositoryBasePath).toURI())
				.resolve(user.getIdentifier())
				.resolve(projectReference.getNamespace())
				.resolve(projectReference.getProjectId())
				.toFile()
				.exists()
		) {
			try (ILocalGitRepositoryManager localRepoManager = localGitRepositoryManager) {
				openProjectListener.progress("Cloning the Git repository");

				// clone the repository locally
				localRepoManager.clone(
					projectReference.getNamespace(),
					projectReference.getProjectId(),
					remoteGitServerManager.getProjectRepositoryUrl(projectReference),
					credentialsProvider
				);
			}
		}
	}

	private String serializeProjectMetadata(String name, String description) {
		JsonObject obj = new JsonObject();
		obj.addProperty(GroupSerializationField.name.name(), name);
		obj.addProperty(GroupSerializationField.description.name(), description);
		return obj.toString();
	}
}
