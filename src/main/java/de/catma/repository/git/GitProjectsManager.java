package de.catma.repository.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.google.common.eventbus.EventBus;
import com.google.gson.JsonObject;

import de.catma.backgroundservice.BackgroundService;
import de.catma.project.ForkStatus;
import de.catma.project.OpenProjectListener;
import de.catma.project.Project;
import de.catma.project.ProjectReference;
import de.catma.project.ProjectsManager;
import de.catma.project.conflict.ConflictedProject;
import de.catma.rbac.RBACPermission;
import de.catma.repository.git.graph.GraphProjectDeletionHandler;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted.GroupSerializationField;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.user.User;
import de.catma.util.IDGenerator;

public class GitProjectsManager implements ProjectsManager {
	private final Logger logger = Logger.getLogger(getClass().getName());
	
	private final ILocalGitRepositoryManager localGitRepositoryManager;
	private final IRemoteGitManagerRestricted remoteGitServerManager;

	private final IDGenerator idGenerator;
    private final BackgroundService backgroundService;
    private String gitBasedRepositoryBasePath;
	private User user;
	private GraphProjectDeletionHandler graphProjectDeletionHandler;
	private final CredentialsProvider credentialsProvider;
	private EventBus eventBus;

	public GitProjectsManager(
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
		this.localGitRepositoryManager = 
				new JGitRepoManager(this.gitBasedRepositoryBasePath, this.user);
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
	private String create(String name, String description) throws IOException {

		String marshalledDescription = marshallProjectMetadata(name, description);
		String human_readable_info = "_" + name.replaceAll("[^\\p{Alnum}]", "_");
		if (human_readable_info.matches("\\_+")) {
			human_readable_info = ""; // no real information value could be gathered from the name
		}
		String projectId = idGenerator.generate() + human_readable_info;

		try (ILocalGitRepositoryManager localGitRepoManager = this.localGitRepositoryManager) {
			// create the remote repository
			CreateRepositoryResponse response = this.remoteGitServerManager.createRepository(
				projectId, marshalledDescription
			);

			// clone the repository locally
			localGitRepoManager.clone(
				user.getIdentifier(),
				projectId,
				response.repositoryHttpUrl,
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
		FileUtils.deleteDirectory(
			Paths.get(this.localGitRepositoryManager.getRepositoryBasePath().getAbsolutePath(), projectId).toFile()
		);

		this.remoteGitServerManager.deleteRepository(projectId);
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
	public List<ProjectReference> getProjectReferences(RBACPermission withPermission) throws Exception {
		return remoteGitServerManager.getProjectReferences(withPermission);
	}

	@Override
	public ProjectReference createProject(String name, String description) throws Exception {
		String projectId = create(name, description);
		
		return new ProjectReference(projectId, user.getIdentifier(), name, description);
	}

	@Override
	public void openProject(
			TagManager tagManager,
			ProjectReference projectReference,
			OpenProjectListener openProjectListener) {
		try {
			
			cloneLocallyIfNotExists(projectReference, openProjectListener);


			Project project =
				new GraphWorktreeProject(
						this.user,
						new GitProjectHandler(
							this.user, 
							projectReference, 
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
	public void leaveProject(ProjectReference projectReference) throws IOException {
		FileUtils.deleteDirectory(
			new File(
				this.localGitRepositoryManager.getRepositoryBasePath(), 
				projectReference.getProjectId())
		);

		this.remoteGitServerManager.leaveProject(
				projectReference.getNamespace(), projectReference.getProjectId());
		
		try {
			graphProjectDeletionHandler.deleteProject(projectReference.getProjectId());
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

	private void cloneLocallyIfNotExists(ProjectReference projectReference, OpenProjectListener openProjectListener) throws Exception {
		if (!Paths.get(new File(this.gitBasedRepositoryBasePath).toURI())
			.resolve(user.getIdentifier())
			.resolve(projectReference.getNamespace())
			.resolve(projectReference.getProjectId())
			.toFile()
			.exists()) {
			try (ILocalGitRepositoryManager localRepoManager = this.localGitRepositoryManager) {
				openProjectListener.progress("Cloning the git repository");
				
				// clone the repository locally
				localRepoManager.clone(
					projectReference.getNamespace(),
					projectReference.getProjectId(),
					remoteGitServerManager.getProjectRepositoryUrl(projectReference),
					credentialsProvider);
				
			}
		}
	}
	
	private String marshallProjectMetadata(String name, String description) {
		JsonObject obj = new JsonObject();
		obj.addProperty(GroupSerializationField.name.name(), name);
		obj.addProperty(GroupSerializationField.description.name(), description);
		return obj.toString();
	}

	public boolean isAuthorizedOnProject(RBACPermission permission, ProjectReference projectReference) {
		return remoteGitServerManager.isAuthorizedOnProject(user, permission, projectReference);
	}
	
	@Override
	public void updateProject(ProjectReference projectReference) throws IOException {
		String marshalledDescription = 
				marshallProjectMetadata(
						projectReference.getName(), projectReference.getDescription());
		
		String projectId = projectReference.getProjectId();
		
		remoteGitServerManager.updateProject(
			projectReference.getNamespace(), projectId, marshalledDescription);
		
	}
	
	@Override
	public ForkStatus forkTagset(TagsetDefinition tagset, String sourceProjectId, ProjectReference targetProject) throws Exception {
	
		String targetProjectId = targetProject.getProjectId();
		String tagsetId = tagset.getUuid();

		cloneLocallyIfNotExists(targetProject, new OpenProjectListener() {
			@Override
			public void ready(Project project) {/** not used **/}
			@Override
			public void conflictResolutionNeeded(ConflictedProject project) {/** not used **/}
			@Override
			public void failure(Throwable t) {/** not used **/}
			@Override
			public void progress(String msg, Object... params) {logger.info(String.format(msg, params));}
		});
		
		GitProjectHandler targetProjectHandler = new GitProjectHandler(
				this.user, 
				targetProject, 
				this.localGitRepositoryManager, 
				this.remoteGitServerManager);
		
		targetProjectHandler.loadRolesPerResource();
		if (targetProjectHandler.hasConflicts()) {
			return ForkStatus.targetHasConflicts();
		}
		else if (targetProjectHandler.hasUncommittedChanges()) {
			return ForkStatus.targetNotClean();
		}
		ForkStatus forkStatus = remoteGitServerManager.forkResource(tagsetId, sourceProjectId, targetProjectId);
		
		if (forkStatus.isSuccess()) {
			targetProjectHandler.cloneAndAddTagset(
					tagset.getUuid(), 
					tagset.getName(),
					String.format(
							"Forked Tagset %1$s with ID %2$s from %3$s with ID %4$s", 
							tagset.getName(), tagset.getUuid(),
							targetProject.getName(),
							targetProjectId));
		}
		else {
			return forkStatus;
		}
		
		
		return ForkStatus.success();
	}
}
