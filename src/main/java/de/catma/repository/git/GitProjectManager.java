package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.google.common.eventbus.EventBus;

import de.catma.backgroundservice.BackgroundService;
import de.catma.project.OpenProjectListener;
import de.catma.project.Project;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.rbac.RBACPermission;
import de.catma.repository.git.graph.GraphProjectDeletionHandler;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.tag.TagManager;
import de.catma.user.User;
import de.catma.util.IDGenerator;
import elemental.json.Json;
import elemental.json.JsonObject;

public class GitProjectManager implements ProjectManager {
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitManagerRestricted remoteGitServerManager;

	private final IDGenerator idGenerator;
    private final BackgroundService backgroundService;
    private String gitBasedRepositoryBasePath;
	private User user;
	private GraphProjectDeletionHandler graphProjectDeletionHandler;
	private final CredentialsProvider credentialsProvider;
	private EventBus eventBus;

	private static final String PROJECT_ROOT_REPOSITORY_NAME_FORMAT = "%s_root";

	public static String getProjectRootRepositoryName(String projectId) {
		return String.format(PROJECT_ROOT_REPOSITORY_NAME_FORMAT, projectId);
	}

	public GitProjectManager(
            String gitBasedRepositoryBasePath,
            IRemoteGitManagerRestricted remoteGitServerManager,
            GraphProjectDeletionHandler graphProjectDeletionHandler,
            BackgroundService backgroundService,
            EventBus eventBus)
					throws IOException {
		this.gitBasedRepositoryBasePath = gitBasedRepositoryBasePath;
		this.remoteGitServerManager = remoteGitServerManager;
		this.graphProjectDeletionHandler = graphProjectDeletionHandler;
		this.backgroundService = backgroundService;
		this.eventBus = eventBus;
		this.user = remoteGitServerManager.getUser();
		this.localGitRepositoryManager = new JGitRepoManager(this.gitBasedRepositoryBasePath, this.user);
		this.credentialsProvider = 
			new UsernamePasswordCredentialsProvider("oauth2", remoteGitServerManager.getPassword());
		this.idGenerator = new IDGenerator();
	}

	/**
	 * Creates a new project.
	 *
	 * @param name the name of the project to create
	 * @param description the description of the project to create
	 * @return the new project ID
	 * @throws IOException if an error occurs when creating the project
	 */
	@Override
	public String create(String name, String description) throws IOException {

		//TODO: consider creating local git projects for offline use
		String marshalledDescription = marshallProjectMetadata(name, description);
		
		String projectId = idGenerator.generate() + "_" + name.replaceAll("[^\\p{Alnum}]", "_");

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the group
			String groupPath = this.remoteGitServerManager.createGroup(
				projectId, projectId, marshalledDescription
			);

			// create the root repository
			String projectNameAndPath = GitProjectManager.getProjectRootRepositoryName(projectId);

			CreateRepositoryResponse response =
					this.remoteGitServerManager.createRepository(
				projectNameAndPath, projectNameAndPath, groupPath
			);

			// clone the root repository locally
			localGitRepoManager.clone(
				projectId,
				response.repositoryHttpUrl,
				null,
				credentialsProvider
			);
		}

		return projectId;
	}



	/**
	 * Deletes an existing project.
	 * <p>
	 * This will also delete any associated repositories automatically (local & remote).
	 *
	 * @param projectId the ID of the project to delete
	 * @throws IOException if an error occurs when deleting the project
	 */
	@Override
	public void delete(String projectId) throws IOException {
		List<String> repositoryNames = this.remoteGitServerManager.getGroupRepositoryNames(
			projectId
		);

		for (String name : repositoryNames) {
			FileUtils.deleteDirectory(
				new File(this.localGitRepositoryManager.getRepositoryBasePath(), name)
			);
		}

		this.remoteGitServerManager.deleteGroup(projectId);
		try {
			graphProjectDeletionHandler.deleteProject(projectId);
		} catch (Exception e) {
			throw new IOException(e);
		};
	}
	
	@Override
	public User getUser() {
		return user;
	}

	@Override
	public List<ProjectReference> getProjectReferences() throws Exception {
		return remoteGitServerManager.getProjectReferences();
	}

	@Override
	public ProjectReference findProjectReferenceById(String projectId) throws IOException {
		return remoteGitServerManager.findProjectReferenceById(Objects.requireNonNull(projectId));
	}

	@Override
	public ProjectReference createProject(String name, String description) throws Exception {
		String projectId = create(name, description);
		
		return new ProjectReference(projectId, name, description);
	}

	@Override
	public void openProject(
			TagManager tagManager,
			ProjectReference projectReference,
			OpenProjectListener openProjectListener) {
		try {
			
			cloneRootLocallyIfNotExists(projectReference, openProjectListener);


			Project project =
				new GraphWorktreeProject(
						this.user,
						new GitProjectHandler(
							this.user, 
							projectReference.getProjectId(), 
							this.localGitRepositoryManager, 
							this.remoteGitServerManager),
						projectReference,
						tagManager,
                        backgroundService,
                        eventBus);
			
			

			project.open(openProjectListener);

			
		} catch (Exception e) {
			openProjectListener.failure(e);
		}
	}

	@Override
	public void leaveProject(String projectId) throws IOException {
		List<String> repositoryNames = this.remoteGitServerManager.getGroupRepositoryNames(
				projectId
			);

			for (String name : repositoryNames) {
				FileUtils.deleteDirectory(
					new File(this.localGitRepositoryManager.getRepositoryBasePath(), name)
				);
			}

			this.remoteGitServerManager.leaveGroup(projectId);
			try {
				graphProjectDeletionHandler.deleteProject(projectId);
			} catch (Exception e) {
				throw new IOException(e);
			};

	}
	
	public boolean existsLocally(ProjectReference projectReference) {
		return Paths.get(new File(this.gitBasedRepositoryBasePath).toURI())
				.resolve(projectReference.getProjectId())
				.toFile()
				.exists();
	}

	private void cloneRootLocallyIfNotExists(ProjectReference projectReference, OpenProjectListener openProjectListener) throws Exception {
		if (!Paths.get(new File(this.gitBasedRepositoryBasePath).toURI())
			.resolve(user.getIdentifier())
			.resolve(projectReference.getProjectId())
			.resolve(GitProjectManager.getProjectRootRepositoryName(projectReference.getProjectId()))
			.toFile()
			.exists()) {
			try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
				openProjectListener.progress("Cloning the git repository");
				
				// clone the repository locally
				localRepoManager.clone(
					projectReference.getProjectId(),
					remoteGitServerManager.getProjectRootRepositoryUrl(projectReference),
					null,
					credentialsProvider,
					true // init and update submodules
				);
				
			}
		}
	}
	
	private String marshallProjectMetadata(String name, String description) {
		JsonObject obj = Json.createObject();
		obj.put("name", name);
		obj.put("description", description);
		return obj.toString();
	}

	public boolean isAuthorizedOnProject(RBACPermission permission, String projectId) {
		return remoteGitServerManager.isAuthorizedOnProject(user, permission, projectId);
	}
	
	@Override
	public void updateProject(ProjectReference projectReference) throws IOException {
		String marshalledDescription = marshallProjectMetadata(projectReference.getName(), projectReference.getDescription());
		
		String projectId = projectReference.getProjectId();
		
		remoteGitServerManager.updateGroup(projectId, projectId, marshalledDescription);
		
	}
}
