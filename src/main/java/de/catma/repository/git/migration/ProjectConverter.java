package de.catma.repository.git.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.Visibility;

import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.GitAnnotationCollectionHandler;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.GitlabUtils;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.Pair;

public class ProjectConverter implements AutoCloseable { 
	
	private final Logger logger = Logger.getLogger(ProjectScanner.class.getName());
	
	private final GitLabApi privilegedGitLabApi;

	private LegacyProjectHandler legacyProjectHandler;

	private Path backupPath;
	

	public ProjectConverter() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		Properties catmaProperties = new Properties();
		catmaProperties.load(new FileInputStream(propertiesFile));

		CATMAProperties.INSTANCE.setProperties(catmaProperties);
		
		this.backupPath = Paths.get(CATMAPropertyKey.Repo6MigrationBackupPath.getValue());
		if (!this.backupPath.toFile().exists()) {
			if (!this.backupPath.toFile().mkdirs()) {
				throw new IllegalStateException(
					String.format("Could not create backup path %1$s", this.backupPath.toString()));
			}
		}
		
		privilegedGitLabApi = new GitLabApi(
				 CATMAPropertyKey.GitLabServerUrl.getValue(), 
				 CATMAPropertyKey.GitLabAdminPersonalAccessToken.getValue()
		);
		
		this.legacyProjectHandler = new LegacyProjectHandler(privilegedGitLabApi);
	}
	
	public void convertProject(String projectId) throws IOException {
		logger.info(String.format("Converting Project %1$s", projectId));

		Path userTempPath = null;
		try {
			logger.info(String.format("Retrieving members of Project %1$s", projectId));
			
			Set<Member> members = this.legacyProjectHandler.getLegacyProjectMembers(projectId);
			Member owner = members.stream()
					.filter(member -> member.getRole().equals(RBACRole.OWNER))
					.findAny()
					.orElse(null);
			if (owner == null) {
				throw new IllegalStateException(
					String.format(
						"Could not find an owner for Project %1$s", projectId));
			}
	
			Pair<User, String> result = this.legacyProjectHandler.aquireUser(owner.getIdentifier());
	
			try (GitLabApi restrictedGitLabApi = 
					new GitLabApi(
							CATMAPropertyKey.GitLabServerUrl.getValue(), 
							result.getSecond())) {
				
				logger.info(String.format("Retrieving legacy Project %1$s (Group)", projectId));
				Group legacyProject = 
					restrictedGitLabApi.getGroupApi().getGroup(projectId);
				
				User ownerUser = result.getFirst();
				
				UsernamePasswordCredentialsProvider credentialsProvider = 
					new UsernamePasswordCredentialsProvider(
							"oauth2", restrictedGitLabApi.getAuthToken());
				
				logger.info(String.format("Creating temp directory for Project %1$s and owner %2$s", projectId, ownerUser));

				String repositoryTempPath = 
						new File(
							CATMAPropertyKey.TempDir.getValue(), 
							"project_migration").getAbsolutePath();
				userTempPath = Paths.get(repositoryTempPath, ownerUser.getIdentifier());
				if (userTempPath.toFile().exists()) {
					this.legacyProjectHandler.deleteUserTempPath(userTempPath);
				}
				
				if (!userTempPath.toFile().mkdirs()) {
					throw new IllegalStateException(
						String.format(
							"Could not create temp directory %1$s", 
							userTempPath.toString()));
				}
	
				String rootRepoName = projectId + "_root";

				try (JGitRepoManager repoManager = 
						new JGitRepoManager(
								repositoryTempPath, 
								result.getFirst())) {

					logger.info(String.format("Cloning Project %1$s", projectId));
					repoManager.cloneWithSubmodules(
							projectId,
							this.legacyProjectHandler.getProjectRootRepositoryUrl(
									restrictedGitLabApi, projectId, rootRepoName),
							credentialsProvider
						);
				}
//				Project project = restrictedGitLabApi.getProjectApi().getProject("mp", projectId);
				
				logger.info(String.format("Creating new target Project %1$s in the owner's namespace %2$s", projectId, ownerUser));
				Project project = restrictedGitLabApi.getProjectApi().createProject(
					projectId, 
					null,
					legacyProject.getDescription(), 
					null,  
					null, 
					null, 
					null, 
					Visibility.PRIVATE, 
					null, 
					null);
				project.setRemoveSourceBranchAfterMerge(false);
				restrictedGitLabApi.getProjectApi().updateProject(project);
				
				Path projectPath = userTempPath.resolve(projectId);
				Path projectBackupPath = backupPath.resolve(projectId);
				
				if (projectBackupPath.toFile().exists() && projectBackupPath.toFile().list().length>0) {
					FileUtils.deleteDirectory(projectBackupPath.toFile());
//					throw new IllegalStateException(
//						String.format(
//							"Project already has a non empty backup at %1$s!", 
//							projectBackupPath.toString()));
				}
				logger.info(String.format("Creating backup for Project %1$s", projectId));

				FileUtils.copyDirectory(projectPath.toFile(), projectBackupPath.toFile());
				
				this.legacyProjectHandler.setUserWritablePermissions(projectPath);
				
				Path projectRootPath = projectPath.resolve(rootRepoName);
				
				try (JGitRepoManager repoManager = 
						new JGitRepoManager(
								repositoryTempPath, 
								result.getFirst())) {
				
					repoManager.open(
							projectId,
							rootRepoName);

					List<String> relativeSubmodulePaths = repoManager.getSubmodulePaths();
					
					logger.info(String.format("Integrating submodule resources into the Project %1$s", projectId));
					for (String relativeSubmodulePath : relativeSubmodulePaths) {
						// create a copy of the submodule
						FileUtils.copyDirectory(
							projectRootPath.resolve(relativeSubmodulePath).toFile(), 
							projectRootPath.resolve(relativeSubmodulePath + "_temp").toFile());
						
						repoManager.removeSubmodule(
							projectRootPath.resolve(relativeSubmodulePath).toFile());
						
						// submodule removal detaches so we have to reopen again
						repoManager.open(
								projectId,
								rootRepoName);

						FileUtils.moveDirectory(
							projectRootPath.resolve(relativeSubmodulePath + "_temp").toFile(),
							projectRootPath.resolve(relativeSubmodulePath).toFile());
						
						if (!projectRootPath.resolve(relativeSubmodulePath).resolve(".git").toFile().delete()) {
							throw new IllegalStateException(
								String.format(
									"Tried to delete .git at %1$s "
									+ "for projectId %2$s "
									+ "but there was none! You need to check "
									+ "before proceeding with the conversion!", 
									projectRootPath.resolve(relativeSubmodulePath).resolve(".git"),
									projectId));
						}
						
						repoManager.add(Paths.get(relativeSubmodulePath));
					}
					
					logger.info(String.format("Removing .gitmodules from Project %1$s", projectId));
					repoManager.remove(
							projectRootPath.resolve(".gitmodules").toFile());
					
					logger.info("Commiting integration of of submodules");
					repoManager.commit(
						"Direct integration of submodules", 
						ownerUser.getIdentifier(), 
						ownerUser.getEmail(), false);
					
					logger.info(
							String.format(
								"Updating remote origin to new target Project %1$s "
								+ "in the owner's namespace %2$s", 
								projectId, ownerUser));
					repoManager.remoteRemove(
							Constants.DEFAULT_REMOTE_NAME);
					repoManager.remoteAdd(
							Constants.DEFAULT_REMOTE_NAME, 
							GitlabUtils.rewriteGitLabServerUrl(project.getHttpUrlToRepo()));

					
					TagLibrary tagLibrary = 
						this.legacyProjectHandler.getTagLibrary(
								repoManager, projectPath.resolve(rootRepoName).toFile(), ownerUser);
					for (String relativeSubmodulePath : relativeSubmodulePaths) {
						if (relativeSubmodulePath.startsWith(GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME)) {
							String collectionId = 
									relativeSubmodulePath.substring(relativeSubmodulePath.indexOf('/')+1);
							convertCollection(
								projectId,
								projectPath.resolve(rootRepoName).toFile(),
								collectionId,
								projectPath
								.resolve(rootRepoName)
								.resolve(GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME)
								.resolve(collectionId)
								.resolve("annotations"),
								tagLibrary,
								repoManager,
								ownerUser);
						}
					}
					
					repoManager.addAllAndCommit(
						"Converted Collections", ownerUser.getIdentifier(), ownerUser.getEmail(), false);
					
					logger.info(
							String.format(
								"Pushing changes to remote origin %1$s/%2$s ",
								ownerUser.getIdentifier(), projectId));
					
					repoManager.push_master(credentialsProvider);
					
					logger.info("Adding team members to the new Project");
					for (Member member : members) {
						if (!member.getIdentifier().equals(ownerUser.getIdentifier())) {
							RBACRole role = member.getRole();
							if (role.getAccessLevel() < RBACRole.ASSISTANT.getAccessLevel()) {
								role = RBACRole.ASSISTANT;
							}
							
							restrictedGitLabApi.getProjectApi().addMember(
									project.getId(), 
									member.getUserId(),
									AccessLevel.forValue(role.getAccessLevel()));
						}
					}
				}
			}
			if (userTempPath != null) {
				logger.info(String.format("Removing temp directory %1$s", userTempPath.toFile()));
				try {
					this.legacyProjectHandler.deleteUserTempPath(userTempPath);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error removing temp file!", e);
				}
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, String.format("Error converting Project %1$s", projectId), e);
		}

	}
	
	private void convertCollection(
		String projectId, File projectDirectory, String collectionId, 
		Path annotationsPath, TagLibrary tagLibrary, JGitRepoManager repoManager, User user) throws Exception {
		logger.info(String.format("Converting Collection %1$s", collectionId));
		if (annotationsPath.toFile().exists() && annotationsPath.toFile().list().length > 0) {
			List<Pair<JsonLdWebAnnotation, TagInstance>> annotations = 
				this.legacyProjectHandler.loadLegacyTagInstances(
					projectId, collectionId, annotationsPath.toFile(), tagLibrary);
			
			GitAnnotationCollectionHandler gitAnnotationCollectionHandler = 
					new GitAnnotationCollectionHandler(
						repoManager, projectDirectory, 
						projectId, user.getIdentifier(), user.getEmail());
			Set<String> annotationsIds =
					annotations.stream()
						.map(entry -> entry.getSecond().getUuid())
						.collect(Collectors.toSet());
			gitAnnotationCollectionHandler.createTagInstances(collectionId, annotations);
			
			for (String annotationId : annotationsIds) {
				File legacyAnnotationFile = annotationsPath.resolve(annotationId + ".json").toFile();
				if (!legacyAnnotationFile.delete()) {
					throw new IllegalStateException(
							String.format(
									"Could not delete legacy Annotation %1$s", 
									legacyAnnotationFile.toString()));
				}
				repoManager.remove(legacyAnnotationFile);
			}
		}
	}

	@Override
	public void close() throws Exception {
		this.privilegedGitLabApi.close();
	}
	
	public static void main(String[] args) throws Exception {
		FileHandler fileHandler = new FileHandler("project_converter.log");
		fileHandler.setFormatter(new SimpleFormatter());
		try (ProjectConverter projectConverter = new ProjectConverter()) {
			projectConverter.convertProject(
				"CATMA_8198C8E3-E653-41DC-984D-44FC23916041_Legacy_of_the_Beast");
		}
		Logger.getLogger("").addHandler(fileHandler);

	}
}
