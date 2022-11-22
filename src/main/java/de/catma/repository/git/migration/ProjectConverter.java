package de.catma.repository.git.migration;

import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.GitAnnotationCollectionHandler;
import de.catma.repository.git.GitLabUtils;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.Pair;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.Pager;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.Visibility;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

public class ProjectConverter implements AutoCloseable { 
	private final Logger logger = Logger.getLogger(ProjectConverter.class.getName());

	private final Path backupPath;
	private final GitLabApi privilegedGitLabApi;
	private final LegacyProjectHandler legacyProjectHandler;

	public ProjectConverter() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ? System.getProperties().getProperty("prop") : "catma.properties";
		Properties catmaProperties = new Properties();
		catmaProperties.load(new FileInputStream(propertiesFile));
		CATMAProperties.INSTANCE.setProperties(catmaProperties);

		this.backupPath = Paths.get(CATMAPropertyKey.V6_REPO_MIGRATION_BACKUP_PATH.getValue());
		if (!this.backupPath.toFile().exists()) {
			if (!this.backupPath.toFile().mkdirs()) {
				throw new IllegalStateException(String.format("Failed to create backup path %s", this.backupPath));
			}
		}

		this.privilegedGitLabApi = new GitLabApi(
				 CATMAPropertyKey.GITLAB_SERVER_URL.getValue(),
				 CATMAPropertyKey.GITLAB_ADMIN_PERSONAL_ACCESS_TOKEN.getValue()
		);

		this.legacyProjectHandler = new LegacyProjectHandler(this.privilegedGitLabApi);
	}

	private boolean hasAnyResources(Path projectPath) {
		return (projectPath.resolve("documents").toFile().exists() && projectPath.resolve("documents").toFile().list().length > 0)
				|| (projectPath.resolve("collections").toFile().exists() && projectPath.resolve("collections").toFile().list().length > 0)
				|| (projectPath.resolve("tagsets").toFile().exists() && projectPath.resolve("tagsets").toFile().list().length > 0);
	}

	public void convertProject(String projectId) {
		// TODO: migrate comments!

		logger.info(String.format("Converting project with ID %s", projectId));

		try {
			logger.info(String.format("Retrieving members of project with ID %s", projectId));
			Set<Member> members = legacyProjectHandler.getLegacyProjectMembers(projectId);
			Member owner = members.stream().filter(member -> member.getRole().equals(RBACRole.OWNER)).findAny().orElse(null);
			if (owner == null) {
				throw new IllegalStateException(String.format("Failed to find an owner for project with ID %s", projectId));
			}

			Pair<User, String> userAndImpersonationToken = legacyProjectHandler.acquireUser(owner.getIdentifier());

			try (GitLabApi restrictedGitLabApi = new GitLabApi(CATMAPropertyKey.GITLAB_SERVER_URL.getValue(), userAndImpersonationToken.getSecond())) {
				logger.info(String.format("Retrieving legacy project (group) with ID %s", projectId));
				Group legacyProject = restrictedGitLabApi.getGroupApi().getGroup(projectId);
				User ownerUser = userAndImpersonationToken.getFirst();
				UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(
						"oauth2", restrictedGitLabApi.getAuthToken()
				);

				logger.info(String.format("Creating temp directory for project with ID %s and owner \"%s\"", projectId, ownerUser.getIdentifier()));
				String migrationTempPath = new File(CATMAPropertyKey.TEMP_DIR.getValue(), "project_migration").getAbsolutePath();
				Path userTempPath = Paths.get(migrationTempPath, ownerUser.getIdentifier()); // this is the same path that JGitRepoManager constructs internally

				if (userTempPath.toFile().exists()) {
					legacyProjectHandler.deleteUserTempPath(userTempPath);
				}

				if (!userTempPath.toFile().mkdirs()) {
					throw new IllegalStateException(String.format("Failed to create temp directory at path %s", userTempPath));
				}

				String rootRepoName = projectId + "_root";

				try (JGitRepoManager repoManager = new JGitRepoManager(migrationTempPath, userAndImpersonationToken.getFirst())) {
					logger.info(String.format("Cloning project with ID %s", projectId));
					repoManager.cloneWithSubmodules(
							projectId,
							legacyProjectHandler.getProjectRootRepositoryUrl(restrictedGitLabApi, projectId, rootRepoName),
							credentialsProvider
					);
				}

				Path projectPath = userTempPath.resolve(projectId);
				Path projectBackupPath = backupPath.resolve(projectId);

				if (projectBackupPath.toFile().exists() && projectBackupPath.toFile().list().length > 0) {
					if (!CATMAPropertyKey.V6_REPO_MIGRATION_OVERWRITE_V6_PROJECT_BACKUP.getBooleanValue()) {
						throw new IllegalStateException(String.format("Project already has a non-empty backup at path %s", projectBackupPath));
					}

					legacyProjectHandler.setUserWritablePermissions(projectBackupPath);
					FileUtils.deleteDirectory(projectBackupPath.toFile());
				}

				logger.info(String.format("Creating backup for project with ID %s", projectId));
				FileUtils.copyDirectory(projectPath.toFile(), projectBackupPath.toFile());

				legacyProjectHandler.setUserWritablePermissions(projectPath);

				Path projectRootPath = projectPath.resolve(rootRepoName);

				try (JGitRepoManager repoManager = new JGitRepoManager(migrationTempPath, userAndImpersonationToken.getFirst())) {
					repoManager.open(projectId, rootRepoName);
					String migrationBranch = CATMAPropertyKey.V6_REPO_MIGRATION_BRANCH.getValue();

					if (!repoManager.hasRemoteRef(Constants.DEFAULT_REMOTE_NAME + "/" + migrationBranch)) {
						logger.warning(
								String.format("Project with ID %s has no migration branch \"%s\" and cannot be converted",	projectId, migrationBranch)
						);
						return;
					}

					repoManager.checkoutFromOrigin(migrationBranch);
					repoManager.initAndUpdateSubmodules(credentialsProvider, new HashSet<>(repoManager.getSubmodulePaths()));

					List<String> relativeSubmodulePaths = repoManager.getSubmodulePaths();

					logger.info(String.format("Integrating submodule resources into project with ID %s", projectId));
					for (String relativeSubmodulePath : relativeSubmodulePaths) {
						// create a copy of the submodule
						File absoluteSubmodulePath = projectRootPath.resolve(relativeSubmodulePath).toFile();
						File absoluteSubmoduleCopyPath = projectRootPath.resolve(relativeSubmodulePath + "_temp").toFile();

						FileUtils.copyDirectory(absoluteSubmodulePath, absoluteSubmoduleCopyPath);

						// remove the submodule
						legacyProjectHandler.setUserWritablePermissions(projectRootPath);
						repoManager.removeSubmodule(absoluteSubmodulePath);

						// submodule removal detaches so we have to reopen again
						repoManager.open(projectId, rootRepoName);

						// move the submodule files back into the repo
						FileUtils.moveDirectory(absoluteSubmoduleCopyPath, absoluteSubmodulePath);

						// delete the old .git file
						File dotGitFile = absoluteSubmodulePath.toPath().resolve(".git").toFile();

						if (!dotGitFile.delete()) {
							throw new IllegalStateException(
								String.format(
										"Tried to delete .git at path %s for project with ID %s but there was none! "
												+ "You need to check before proceeding with the conversion!",
										dotGitFile,
										projectId
								)
							);
						}

						// add the files for what was once the submodule
						repoManager.add(Paths.get(relativeSubmodulePath));
					}

					logger.info(String.format("Deleting .gitmodules from project with ID %s", projectId));
					repoManager.remove(projectRootPath.resolve(".gitmodules").toFile());

					logger.info("Committing integration of submodules");
					repoManager.commit("Direct integration of submodules", ownerUser.getIdentifier(), ownerUser.getEmail(), false);

					if (!hasAnyResources(projectRootPath)) {
						logger.warning(String.format("Project with ID %s does not seem to have any resources, skipping conversion", projectId));
						return;
					}

					logger.info(String.format("Creating new target project with ID %s in the owner's namespace \"%s\"", projectId, ownerUser.getIdentifier()));
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
							null
					);
					project.setRemoveSourceBranchAfterMerge(false);
					restrictedGitLabApi.getProjectApi().updateProject(project);

					logger.info(
							String.format(
									"Updating remote 'origin' to new target project with ID %s in the owner's namespace \"%s\"",
									projectId,
									ownerUser.getIdentifier()
							)
					);
					repoManager.remoteRemove(Constants.DEFAULT_REMOTE_NAME);
					repoManager.remoteAdd(Constants.DEFAULT_REMOTE_NAME, GitLabUtils.rewriteGitLabServerUrl(project.getHttpUrlToRepo()));

					// convert collections to the new storage layout
					TagLibrary tagLibrary = legacyProjectHandler.getTagLibrary(repoManager, projectRootPath.toFile(), ownerUser);

					for (String relativeSubmodulePath : relativeSubmodulePaths) {
						if (relativeSubmodulePath.startsWith(GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME)) {
							String collectionId = relativeSubmodulePath.substring(relativeSubmodulePath.indexOf('/') + 1);
							convertCollection(
									projectId,
									projectRootPath.toFile(),
									collectionId,
									projectRootPath.resolve(GitProjectHandler.ANNOTATION_COLLECTIONS_DIRECTORY_NAME).resolve(collectionId)
											.resolve(GitAnnotationCollectionHandler.ANNNOTATIONS_DIR),
									tagLibrary,
									repoManager,
									ownerUser
							);
						}
					}

					repoManager.addAllAndCommit("Converted annotation collections", ownerUser.getIdentifier(), ownerUser.getEmail(), false);

					MergeResult mergeResult = null;

					if (repoManager.hasRef(Constants.MASTER)) {
						repoManager.checkout(Constants.MASTER);
						mergeResult = repoManager.merge(migrationBranch);
					}
					else {
						repoManager.checkoutNewFromBranch(Constants.MASTER, migrationBranch);
					}

					if (mergeResult != null && !mergeResult.getMergeStatus().isSuccessful()) {
						logger.severe(String.format("Failed to merge \"%s\" into \"%s\": %s", migrationBranch, Constants.MASTER, mergeResult));
						return;
					}

					logger.info("Pushing converted project (master branch)");
					repoManager.pushMaster(credentialsProvider);

					logger.info("Adding original team members to the new project");
					for (Member member : members) {
						if (member.getIdentifier().equals(ownerUser.getIdentifier())) {
							continue;
						}

						RBACRole role = member.getRole();
						if (role.getAccessLevel() < RBACRole.ASSISTANT.getAccessLevel()) {
							role = RBACRole.ASSISTANT; // this is the lowest role now for CATMA projects
						}

						restrictedGitLabApi.getProjectApi().addMember(
								project.getId(),
								member.getUserId(),
								AccessLevel.forValue(role.getAccessLevel())
						);
					}

					if (CATMAPropertyKey.V6_REPO_MIGRATION_CLEANUP_CONVERTED_V6_PROJECT.getBooleanValue()) {
						logger.info(String.format("Deleting legacy project (group) with ID %s", projectId));
						restrictedGitLabApi.getGroupApi().deleteGroup(projectId);

						logger.info(String.format("Deleting local Git repositories for project with ID %s", projectId));
						File gitRepositoryBaseDir = new File(CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue());

						for (File userDir : gitRepositoryBaseDir.listFiles()) {
							for (File projectDir : userDir.listFiles()) {
								if (projectDir.getName().equals(projectId)) {
									legacyProjectHandler.setUserWritablePermissions(Paths.get(projectDir.getAbsolutePath()));

									try {
										FileUtils.deleteDirectory(projectDir);
									}
									catch (FileSystemException fse) {
										logger.log(Level.WARNING, String.format("Couldn't clean up project directory at path %s", projectDir), fse);
									}
								}
							}
						}
					}
				}

				if (CATMAPropertyKey.V6_REPO_MIGRATION_REMOVE_USER_TEMP_DIRECTORY.getBooleanValue()) {
					logger.info(String.format("Deleting temp directory at path %s", userTempPath.toFile()));

					try {
						legacyProjectHandler.deleteUserTempPath(userTempPath);
					}
					catch (Exception e) {
						logger.log(Level.WARNING, String.format("Couldn't clean up user temp directory at path %s", userTempPath), e);
					}
				}
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, String.format("Error converting project with ID %s", projectId), e);
		}
	}

	public void convertProjects(int limit) throws Exception {
		logger.info("Converting projects...");

		boolean hasLimit = limit != -1;

		Pager<Group> pager = legacyProjectHandler.getLegacyProjectReferences();

		while (pager.hasNext()) {
			for (Group group : pager.next()) {
				if (hasLimit && limit <= 0) {
					return;
				}

				// we are only interested in group-based CATMA projects
				if (group.getName().startsWith("CATMA")) {
					convertProject(group.getName());
					limit--;
				}
			}
		}
	}

	private void convertCollection(
			String projectId, File projectDirectory,
			String collectionId, Path annotationsPath,
			TagLibrary tagLibrary, JGitRepoManager repoManager, User user
	) throws Exception {
		logger.info(String.format("Converting collection with ID %s", collectionId));

		if (annotationsPath.toFile().exists() && annotationsPath.toFile().list().length > 0) {
			List<Pair<JsonLdWebAnnotation, TagInstance>> annotations = legacyProjectHandler.loadLegacyTagInstances(
					projectId, collectionId, annotationsPath.toFile(), tagLibrary
			);
			Set<String> annotationIds = annotations.stream().map(entry -> entry.getSecond().getUuid()).collect(Collectors.toSet());

			GitAnnotationCollectionHandler gitAnnotationCollectionHandler =	new GitAnnotationCollectionHandler(
					repoManager, projectDirectory, projectId, user.getIdentifier(), user.getEmail()
			);
			gitAnnotationCollectionHandler.createTagInstances(collectionId, annotations);

			for (String annotationId : annotationIds) {
				File legacyAnnotationFile = annotationsPath.resolve(annotationId + ".json").toFile();
				if (!legacyAnnotationFile.delete()) {
					throw new IllegalStateException(String.format("Failed to delete legacy annotation file at path %s", legacyAnnotationFile));
				}
				repoManager.remove(legacyAnnotationFile);
			}
		}
	}

	@Override
	public void close() throws Exception {
		privilegedGitLabApi.close();
	}

	public static void main(String[] args) throws Exception {
		FileHandler fileHandler = new FileHandler("project_converter.log");
		fileHandler.setFormatter(new SimpleFormatter());
		Logger.getLogger("").addHandler(fileHandler);

		try (ProjectConverter projectConverter = new ProjectConverter()) {
			String projectList = CATMAPropertyKey.V6_REPO_MIGRATION_PROJECT_ID_LIST.getValue();

			if ((projectList != null) && !projectList.isEmpty()) {
				for (String projectId : projectList.split(",")) {
					projectConverter.convertProject(projectId);
				}
			}
			else {
				int limit = CATMAPropertyKey.V6_REPO_MIGRATION_MAX_PROJECTS.getIntValue();
				projectConverter.convertProjects(limit);
			}
		}
	}
}
