package de.catma.repository.git.migration;

import com.beust.jcommander.internal.Maps;
import de.catma.project.CommitInfo;
import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.managers.JGitCredentialsManager;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.Pair;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.StoredConfig;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.Pager;
import org.gitlab4j.api.models.Group;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ProjectScanner implements AutoCloseable {
	private final Logger logger = Logger.getLogger(ProjectScanner.class.getName());

	private static final String SCAN_RESULTS_FILE_NAME = "project_scan_result.csv";
	private final String migrationBranchName;
	private final HashSet<String> projectIdsToSkipInByProjectScanMode;

	private final GitLabApi privilegedGitLabApi;
	private final LegacyProjectHandler legacyProjectHandler;

	private final Map<String, ProjectReport> projectReportsById;

	private static final String SYSTEM_COMMITTER_NAME = "CATMA System";
	private static final String SYSTEM_COMMITTER_EMAIL = "support@catma.de";

	public ProjectScanner() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ? System.getProperties().getProperty("prop") : "catma.properties";
		Properties catmaProperties = new Properties();
		catmaProperties.load(new FileInputStream(propertiesFile));
		CATMAProperties.INSTANCE.setProperties(catmaProperties);

		this.migrationBranchName = CATMAPropertyKey.V6_REPO_MIGRATION_BRANCH.getValue();

		String projectIdsToSkipSettingValue = CATMAPropertyKey.V6_REPO_MIGRATION_PROJECT_ID_LIST_TO_SKIP.getValue();
		if (projectIdsToSkipSettingValue == null || projectIdsToSkipSettingValue.isEmpty()) {
			this.projectIdsToSkipInByProjectScanMode = new HashSet<>();
		}
		else {
			this.projectIdsToSkipInByProjectScanMode = new HashSet<>(Arrays.asList(projectIdsToSkipSettingValue.split(",")));
		}

		this.privilegedGitLabApi = new GitLabApi(
				 CATMAPropertyKey.GITLAB_SERVER_URL.getValue(),
				 CATMAPropertyKey.GITLAB_ADMIN_PERSONAL_ACCESS_TOKEN.getValue()
		);
		this.legacyProjectHandler = new LegacyProjectHandler(this.privilegedGitLabApi);

		this.projectReportsById = Maps.newHashMap();
	}

	private ProjectReport getProjectReport(String projectId) {
		ProjectReport projectReport = projectReportsById.get(projectId);
		if (projectReport == null) {
			projectReport = new ProjectReport(projectId);
			projectReportsById.put(projectId, projectReport);
		}
		return projectReport;
	}

	private boolean isValidLegacyProject(File projectDir, List<Group> projects) {
		for (Group project : projects) {
			if (project.getName().equals(projectDir.getName())) {
				return true;
			}
		}

		return false;
	}

	private void scanProject(
			String projectId, 
			JGitRepoManager userRepoManager,
			JGitCredentialsManager jGitCredentialsManager,
			GitLabApi restrictedGitLabApi,
			User user
	) {
		try (JGitRepoManager repoManager = userRepoManager) {
			logger.info(String.format("Scanning project with ID %s", projectId));

			// get the list of submodules/resources
			String rootRepoName = projectId + "_root";
			repoManager.open(projectId, rootRepoName);

			// for debugging on Windows
//			StoredConfig repositoryConfig = repoManager.getGitApi().getRepository().getConfig();
//			repositoryConfig.setBoolean("core", null, "filemode", false);
//			repositoryConfig.save();

			List<String> resources = repoManager.getSubmodulePaths();
			repoManager.detach();

			List<String> readableResources = new ArrayList<>();

			for (String resource : resources) {
				RBACRole role = legacyProjectHandler.getLegacyResourcePermissions(restrictedGitLabApi, projectId, resource);

				if (role.equals(RBACRole.GUEST)) {
					logger.info(String.format(
							"User \"%1$s\" only has the 'Guest' role on resource at path %2$s/%3$s, skipping", repoManager.getUsername(), projectId, resource
					));
					continue;
				}

				readableResources.add(resource);

				// open the submodule repo and check out the dev branch
				repoManager.open(projectId, rootRepoName + "/" + resource);

				// for debugging on Windows
//				StoredConfig submoduleRepositoryConfig = repoManager.getGitApi().getRepository().getConfig();
//				submoduleRepositoryConfig.setBoolean("core", null, "filemode", false);
//				submoduleRepositoryConfig.save();

				repoManager.checkout("dev", true);

				// get the Git status and add it to the project report
				Status status = repoManager.getStatus();
				getProjectReport(projectId).addStatus(resource, status);

				// get the HEAD commit and add it to the project report
				CommitInfo headCommit = repoManager.getHeadCommit();
				if (headCommit != null) {
					getProjectReport(projectId).addLastHeadCommit(headCommit);
				}

				// if the Git status is clean, check what still needs to, and can be, merged and add these details to the project report
				if (status.isClean()) {
					logger.info(String.format("Fetching resource at path %s/%s", projectId, resource));
					repoManager.fetch(jGitCredentialsManager);

					List<CommitInfo> commitsDevIntoMaster = repoManager.getCommitsNeedToBeMergedFromDevToMaster();
					getProjectReport(projectId).addNeedMergeDevToMaster(resource, commitsDevIntoMaster);

					if (!commitsDevIntoMaster.isEmpty()) {
						boolean canMergeDevIntoMaster = repoManager.canMerge(Constants.MASTER);
						getProjectReport(projectId).addCanMergeDevToMaster(resource, canMergeDevIntoMaster);
					}

					List<CommitInfo> commitsMasterIntoOriginMaster = repoManager.getCommitsNeedToBeMergedFromMasterToOriginMaster();
					getProjectReport(projectId).addNeedMergeMasterToOriginMaster(resource, commitsMasterIntoOriginMaster);

					if (!commitsMasterIntoOriginMaster.isEmpty() || !commitsDevIntoMaster.isEmpty()) {
						// we are checking dev against origin/master here because ultimately that's where all dev commits need to end up without conflicts
						getProjectReport(projectId).addCanMergeDevToOriginMaster(
								resource,
								repoManager.canMerge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER)
						);
					}
				}

				repoManager.detach();
			}

			// open the root repo
			repoManager.open(projectId, rootRepoName);

			// get the Git status and add it to the project report
			Status rootStatus = repoManager.getStatus();
			getProjectReport(projectId).addStatus(projectId, rootStatus);

			// get the HEAD commit and add it to the project report
			CommitInfo headCommit = repoManager.getHeadCommit();
			if (headCommit != null) {
				getProjectReport(projectId).addLastHeadCommit(headCommit);
			}

			// if the Git status is clean, check what still needs to, and can be, merged and add these details to the project report
			if (rootStatus.isClean()) {
				logger.info(String.format("Performing fetch on root repo for project with ID %s", projectId));
				repoManager.fetch(jGitCredentialsManager);

				List<CommitInfo> commitsMasterIntoOriginMaster = repoManager.getCommitsNeedToBeMergedFromMasterToOriginMaster();
				getProjectReport(projectId).addNeedMergeMasterToOriginMaster(projectId, commitsMasterIntoOriginMaster);

				if (!commitsMasterIntoOriginMaster.isEmpty()) {
					getProjectReport(projectId).addCanMergeMasterToOriginMaster(
						projectId, 
						repoManager.canMerge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER)
					);
				}
			}

			repoManager.detach();

			if (CATMAPropertyKey.V6_REPO_MIGRATION_SCAN_WITH_MERGE_AND_PUSH.getBooleanValue()) {
				// merge and push submodules/resources
				for (String resource : readableResources) {
					repoManager.open(projectId, rootRepoName + "/" + resource);

					Status status = repoManager.getStatus();
					if (status.isClean()) {
						mergeAndPushResource(repoManager, jGitCredentialsManager, projectId, resource);
					}

					repoManager.detach();
				}

				 // if we had a clean rootStatus before merging resources we merge and push the root repo
				if (rootStatus.isClean()) { 
					repoManager.open(projectId, rootRepoName);
					mergeAndPushRoot(repoManager, user, jGitCredentialsManager, projectId, rootRepoName);
					repoManager.detach();
				}
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, String.format("Error scanning project with ID %s", projectId), e);
			getProjectReport(projectId).addError(e);
		}
	}

	private boolean resolveRootConflicts(
			JGitRepoManager repoManager, 
			User user,
			JGitCredentialsManager jGitCredentialsManager,
			MergeResult mergeResult,
			String projectId,
			String rootRepoName
	) throws Exception {
		boolean clean = true;

		// if the merge wasn't successful, attempt to resolve conflicts
		if (!mergeResult.getMergeStatus().isSuccessful()) {
			if (mergeResult.getConflicts().containsKey(Constants.DOT_GIT_MODULES)) {
				logger.info(String.format("Found conflicts in .gitmodules of project with ID %s, trying auto resolution", projectId));
				repoManager.resolveGitSubmoduleFileConflicts();
			}

			clean = repoManager.resolveRootConflicts(jGitCredentialsManager);

			// if conflicts could be resolved, re-add all submodules
			if (clean) {
				List<String> relativeSubmodulePaths = repoManager.getSubmodulePaths();

				for (String relativeSubmodulePath : relativeSubmodulePaths) {
					Path submodulePath = Paths.get(repoManager.getRepositoryWorkTree().getAbsolutePath(), relativeSubmodulePath);
					String remoteUrl = String.format(
							"%s/%s/%s.git", CATMAPropertyKey.GITLAB_SERVER_URL.getValue(), projectId, submodulePath.getFileName().toString()
					);
					repoManager.reAddSubmodule(submodulePath.toFile(), remoteUrl, jGitCredentialsManager);
					repoManager.initAndUpdateSubmodules(jGitCredentialsManager, Collections.singleton(relativeSubmodulePath));
					repoManager.detach();

					// check out the remote migration branch for the submodule
					repoManager.open(projectId, rootRepoName + "/" + relativeSubmodulePath);
					repoManager.checkoutFromOrigin(migrationBranchName);
					repoManager.detach();

					// re-open the root repo
					repoManager.open(projectId, rootRepoName);
				}

				repoManager.addAllAndCommit(
						"Auto-committing merged changes (resolveRootConflicts)",
						user.getIdentifier(),
						user.getEmail(),
						true
				);
			}
		}

		return clean;
	}

	private void mergeAndPushRoot(
			JGitRepoManager repoManager,
			User user,
			JGitCredentialsManager jGitCredentialsManager,
			String projectId,
			String rootRepoName
	) throws Exception {
		if (!repoManager.hasRef(Constants.MASTER)) {
			logger.warning(
					String.format(
							"The root repo of the project with ID %s for user \"%s\" doesn't have a master branch (no commits)",
							projectId,
							user.getIdentifier()
					)
			);
			return;
		}

		repoManager.checkout(migrationBranchName, true);

		Status status = repoManager.getStatus();
		if (!status.isClean()) {
			repoManager.addAllAndCommit(
					"Auto-committing merged resource changes",
					user.getIdentifier(),
					user.getEmail(),
					false
			);
		}

		repoManager.fetch(jGitCredentialsManager);

		boolean hasRemoteMigrationBranch = repoManager.hasRef(Constants.DEFAULT_REMOTE_NAME + "/" + migrationBranchName);
		boolean clean = true;

		// if the migration branch exists remotely, merge it into the local migration branch
		if (hasRemoteMigrationBranch) {
			MergeResult mrOriginC6MigrationIntoC6Migration;

			try {
				mrOriginC6MigrationIntoC6Migration = repoManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + migrationBranchName);
			}
			catch (IOException e) {
				if (e.getMessage().contains("Failed to merge branch") && e.getCause() instanceof CheckoutConflictException) {
					String checkoutConflictExceptionMessage = e.getCause().getMessage();
					status = repoManager.getStatus();
					if (!status.getUntracked().isEmpty()
							&& status.getRemoved().containsAll(status.getUntracked())
							&& status.getUntracked().stream().allMatch(checkoutConflictExceptionMessage::contains)
					) {
						// handles the case where a merge is aborted because of untracked deleted submodules
						for (String relativeSubmodulePath : status.getUntracked()) {
							String unixStyleRelativeSubmodulePath = FilenameUtils.separatorsToUnix(relativeSubmodulePath);
							File submoduleDir = repoManager.getRepositoryWorkTree().toPath().resolve(unixStyleRelativeSubmodulePath).toFile();
							FileUtils.deleteDirectory(submoduleDir);
						}

						// commit the merge
						repoManager.commit(
								String.format("Merge remote-tracking branch '%1$s/%2$s' into %2$s", Constants.DEFAULT_REMOTE_NAME, migrationBranchName),
								SYSTEM_COMMITTER_NAME,
								SYSTEM_COMMITTER_EMAIL,
								false
						);

						// get a fresh MergeResult
						mrOriginC6MigrationIntoC6Migration = repoManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + migrationBranchName);
					}
					else {
						throw e;
					}
				}
				else {
					throw e;
				}
			}

			getProjectReport(projectId).addMergeResultOriginC6MigrationToC6Migration(projectId, mrOriginC6MigrationIntoC6Migration);
			clean = resolveRootConflicts(repoManager, user, jGitCredentialsManager, mrOriginC6MigrationIntoC6Migration, projectId, rootRepoName);
		}

		// if the previous merge succeeded or all conflicts could be resolved
		if (clean) {
			// merge origin/master into the local migration branch
			if (repoManager.hasRemoteRef(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER)) {
				MergeResult mrOriginMasterIntoC6Migration = repoManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER);
				getProjectReport(projectId).addMergeResultOriginMasterToC6Migration(projectId, mrOriginMasterIntoC6Migration);
				clean = resolveRootConflicts(repoManager, user, jGitCredentialsManager, mrOriginMasterIntoC6Migration, projectId, rootRepoName);
			}

			// if the previous merge succeeded or all conflicts could be resolved
			if (clean) {
				// merge the local master branch into the local migration branch
				MergeResult mrMasterIntoC6Migration = repoManager.merge(Constants.MASTER);
				getProjectReport(projectId).addMergeResulMasterToC6Migration(projectId, mrMasterIntoC6Migration);
				clean = resolveRootConflicts(repoManager, user, jGitCredentialsManager, mrMasterIntoC6Migration, projectId, rootRepoName);

				// if the previous merge succeeded or all conflicts could be resolved
				if (clean) {
					// push the migration branch
					repoManager.pushWithoutBranchChecks(jGitCredentialsManager);

					// check if we have any local commits that didn't make it into the remote migration branch and update the project report accordingly
					List<CommitInfo> commits = repoManager.getCommitsNeedToBeMergedFromC6MigrationToOriginC6Migration(migrationBranchName);
					if (commits.size() > 0) {
						getProjectReport(projectId).addPushC6MigrationToOriginC6MigrationFailed(projectId);
					}
					else {
						getProjectReport(projectId).addPushC6MigrationToOriginC6MigrationSuccessful(projectId);
						getProjectReport(projectId).addReadyToMigrateRoot(projectId);
					}
				}
			}
		}
	}

	private void mergeAndPushResource(
			JGitRepoManager repoManager,
			JGitCredentialsManager jGitCredentialsManager,
			String projectId,
			String resource
	) throws Exception {
		repoManager.checkout(migrationBranchName, true);
		repoManager.fetch(jGitCredentialsManager);

		boolean hasRemoteMigrationBranch = repoManager.hasRef(Constants.DEFAULT_REMOTE_NAME + "/" + migrationBranchName);

		// if the migration branch exists remotely, merge it into the local migration branch
		MergeResult mrOriginC6MigrationIntoC6Migration = null;
		if (hasRemoteMigrationBranch) {
			mrOriginC6MigrationIntoC6Migration = repoManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + migrationBranchName);
			getProjectReport(projectId).addMergeResultOriginC6MigrationToC6Migration(resource, mrOriginC6MigrationIntoC6Migration);
		}

		// if the migration branch does not exist remotely, or the previous merge succeeded
		if (!hasRemoteMigrationBranch || mrOriginC6MigrationIntoC6Migration.getMergeStatus().isSuccessful()) {
			// merge origin/master into the local migration branch
			MergeResult mrOriginMasterIntoC6Migration = repoManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER);
			getProjectReport(projectId).addMergeResultOriginMasterToC6Migration(resource, mrOriginMasterIntoC6Migration);

			// if the previous merge succeeded
			if (mrOriginMasterIntoC6Migration.getMergeStatus().isSuccessful()) {
				// merge the local dev branch into the local migration branch
				MergeResult mrDevIntoC6Migration = repoManager.merge("dev");
				getProjectReport(projectId).addMergeResultDevToC6Migration(resource, mrDevIntoC6Migration);

				// if the previous merge succeeded
				if (mrDevIntoC6Migration.getMergeStatus().isSuccessful()) {
					// push the migration branch
					repoManager.pushWithoutBranchChecks(jGitCredentialsManager);

					// check if we have any local commits that didn't make it into the remote migration branch and update the project report accordingly
					List<CommitInfo> commits = repoManager.getCommitsNeedToBeMergedFromC6MigrationToOriginC6Migration(migrationBranchName);
					if (commits.size() > 0) {
						getProjectReport(projectId).addPushC6MigrationToOriginC6MigrationFailed(resource);
					}
					else {
						getProjectReport(projectId).addPushC6MigrationToOriginC6MigrationSuccessful(resource);
					}
				}
			}
		}
	}

	private void scanUserProjects(String username, String projectId, String authenticationUsername) {
		logger.info(String.format("Scanning user \"%s\" project(s): %s", username, projectId == null ? "all" : projectId));

		Path userTempPath = null;

		try {
			// when projectId is null, i.e. when called from scanUser, the user counts for the individual projects could be wrong
			// unless we scan ALL users - however in this scenario the user counts are added to the "null" project only and indicate
			// the total no. of users scanned/stale
			getProjectReport(projectId).addUser(username);

			Pair<User, String> userAndImpersonationToken = legacyProjectHandler.acquireUser(username);
			if (userAndImpersonationToken == null) {
				getProjectReport(projectId).addStaleUser(username);
				return;
			}
			User user = userAndImpersonationToken.getFirst();

			Pair<User, String> authenticatingUserAndImpersonationToken = legacyProjectHandler.acquireUser(authenticationUsername);
			String authenticatingUserImpersonationToken = authenticatingUserAndImpersonationToken.getSecond();

			try (GitLabApi restrictedGitLabApi = new GitLabApi(CATMAPropertyKey.GITLAB_SERVER_URL.getValue(), authenticatingUserImpersonationToken)) {
				JGitCredentialsManager jGitCredentialsManager = new JGitCredentialsManager(new GitUserInformationProviderMigrationImpl(restrictedGitLabApi));

				String repositoryBasePath = CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue();
				Path userBasePath = Paths.get(new File(repositoryBasePath).getAbsolutePath(), username);

				String migrationTempPath = new File(CATMAPropertyKey.TEMP_DIR.getValue(), "project_migration").getAbsolutePath();
				userTempPath = Paths.get(migrationTempPath, username); // this is the same path that JGitRepoManager constructs internally

				if (userTempPath.toFile().exists()) {
					legacyProjectHandler.deleteUserTempPath(userTempPath);
				}

				if (!userTempPath.toFile().mkdirs()) {
					throw new IllegalStateException(String.format("Failed to create temp directory at path %s", userTempPath));
				}

				Path copySrc = userBasePath;
				Path copyDest = userTempPath;
				if (projectId != null) {
					copySrc = copySrc.resolve(projectId);
					copyDest = copyDest.resolve(projectId);
				}

				if (!copySrc.toFile().exists()) {
					logger.info(String.format("Project with ID %s hasn't been cloned by user \"%s\", skipping", projectId, username));
					return;
				}

				logger.info(String.format("Creating temp directory at path %s", copyDest));
				FileUtils.copyDirectory(copySrc.toFile(), copyDest.toFile());

				try (JGitRepoManager repoManager = new JGitRepoManager(migrationTempPath, user)) {
					File[] projectDirs = userTempPath.toFile().listFiles(file -> file.isDirectory() && file.getName().startsWith("CATMA"));
					List<Group> projects = legacyProjectHandler.getLegacyUserProjectReferences(restrictedGitLabApi);

					for (File projectDir : projectDirs) {
						if (!isValidLegacyProject(projectDir, projects)) {
							logger.info(String.format("Found stale project for user \"%s\": %s", username, projectDir.getName()));
							getProjectReport(projectId).addStaleProject(projectDir);
							continue;
						}

						scanProject(projectDir.getName(), repoManager, jGitCredentialsManager, restrictedGitLabApi, user);
					}
				}
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, String.format("Error scanning user projects for user \"%s\"", username), e);
		}
		finally {
			if (userTempPath != null && CATMAPropertyKey.V6_REPO_MIGRATION_REMOVE_USER_TEMP_DIRECTORY.getBooleanValue(true)) {
				logger.info(String.format("Deleting temp directory at path %s", userTempPath));
				try {
					legacyProjectHandler.deleteUserTempPath(userTempPath);
				}
				catch (Exception e) {
					logger.log(Level.SEVERE, String.format("Failed to delete temp directory at path %s", userTempPath), e);
				}
			}
		}
		
	}

	public void scanProject(String projectId) throws IOException {
		if (projectIdsToSkipInByProjectScanMode.contains(projectId)) {
			logger.info(String.format("Skipping project with ID %s", projectId));
			return;
		}

		logger.info(String.format("Scanning project with ID %s", projectId));

		Set<Member> members = legacyProjectHandler.getLegacyProjectMembers(projectId);
		Member owner = members.stream().filter(member -> member.getRole().equals(RBACRole.OWNER)).findAny().orElse(null);

		if (owner == null) {
			logger.severe(String.format("Project with ID %s has no owner, skipping", projectId));
			return;
		}

		getProjectReport(projectId).setOwnerEmail(owner.getEmail() == null ? owner.getIdentifier() : owner.getEmail());

//		legacyProjectHandler.removeC6MigrationBranches(projectId, migrationBranchName);

		for (Member member : members) {
			// we use the owner for authentication, since merging requires full access to all resources
			scanUserProjects(member.getIdentifier(), projectId, owner.getIdentifier());
		}

		exportProjectReport(getProjectReport(projectId));
		logger.info(getProjectReport(projectId).toString());
	}

	public void scanProjects(int limit) throws Exception {
		logger.info(String.format("Scanning projects with limit=%d", limit));

		boolean hasLimit = limit != -1;

		Pager<Group> pager = legacyProjectHandler.getLegacyProjectReferences();

		while (pager.hasNext()) {
			for (Group group : pager.next()) {
				if (hasLimit && limit <= 0) {
					return;
				}

				// we are only interested in group-based CATMA projects
				if (group.getName().startsWith("CATMA")) {
					scanProject(group.getName());
					limit--;
				}
			}
		}
	}

	public void scanUser(String username) {
		scanUserProjects(username, null, username);
	}

	public void scanUsers(int limit) {
		logger.info(String.format("Scanning users with limit=%d", limit));

		File repositoryBasePath = new File(CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue());
		File[] userDirs = repositoryBasePath.listFiles(file -> file.isDirectory());

		if (limit == -1) {
			limit = userDirs.length;
		}

		for (File userDir : userDirs) {
			if (limit <= 0) {
				break;
			}
			scanUser(userDir.getName());
			limit--;
		}
	}

	public void exportAndLogAllProjectReports() {
		for (ProjectReport projectReport : projectReportsById.values()) {
			exportProjectReport(projectReport);
			logger.info(projectReport.toString());
		}
	}

	public void logSummaryProjectReport() {
		logger.info("\n\n\nSummary Project Scan Report:\n\n" + new ProjectReport(projectReportsById.values()));
	}

	private void exportProjectReport(ProjectReport projectReport) {
		try {
			File scanResultsFile = new File(SCAN_RESULTS_FILE_NAME);
			boolean addHeader = !scanResultsFile.exists();

			OutputStreamWriter writer = new OutputStreamWriter(
					new FileOutputStream(SCAN_RESULTS_FILE_NAME, true),
					StandardCharsets.UTF_8
			);

			try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.withDelimiter(';'))) {
				if (addHeader) {
					ProjectReport.exportHeaderToCsv(csvPrinter);
				}
				projectReport.exportToCsv(csvPrinter);
				csvPrinter.flush();
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Error exporting scan result", e);
		}
	}

	@Override
	public void close() throws Exception {
		privilegedGitLabApi.close();
	}

	public static void main(String[] args) throws Exception {
		FileHandler fileHandler = new FileHandler("project_scanner.log");
		fileHandler.setFormatter(new SimpleFormatter());
		Logger.getLogger("").addHandler(fileHandler);

		try (final ProjectScanner projectScanner = new ProjectScanner()) {
			Thread finalReportHook = new Thread() {
				public void run() {
					projectScanner.logSummaryProjectReport();
				}
			};
			Runtime.getRuntime().addShutdownHook(finalReportHook);

			File scanResultsFile = new File(SCAN_RESULTS_FILE_NAME);
			if (scanResultsFile.exists()) {
				scanResultsFile.delete();
			}

			ScanMode scanMode = ScanMode.valueOf(CATMAPropertyKey.V6_REPO_MIGRATION_SCAN_MODE.getValue());

			switch (scanMode) {
				case ByUser:
					String userList = CATMAPropertyKey.V6_REPO_MIGRATION_USER_LIST.getValue();

					if ((userList != null) && !userList.isEmpty()) {
						for (String user : userList.split(",")) {
							projectScanner.scanUser(user.trim());
						}
					}
					else {
						int limit = CATMAPropertyKey.V6_REPO_MIGRATION_MAX_USERS.getIntValue();
						projectScanner.scanUsers(limit);
					}
					projectScanner.exportAndLogAllProjectReports(); // for ByProject scans, scanProject does this as each project is scanned
					break;
				case ByProject:
					String projectList = CATMAPropertyKey.V6_REPO_MIGRATION_PROJECT_ID_LIST.getValue();

					if ((projectList != null) && !projectList.isEmpty()) {
						for (String projectId : projectList.split(",")) {
							projectScanner.scanProject(projectId);
						}
					}
					else {
						int limit = CATMAPropertyKey.V6_REPO_MIGRATION_MAX_PROJECTS.getIntValue();
						projectScanner.scanProjects(limit);
					}
					break;
			}

			projectScanner.logSummaryProjectReport();

			// if we got that ^ far this hook is no longer necessary
			Runtime.getRuntime().removeShutdownHook(finalReportHook);
		}
	}
}
