package de.catma.repository.git.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.Pager;
import org.gitlab4j.api.models.Group;

import com.beust.jcommander.internal.Maps;

import de.catma.project.CommitInfo;
import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.Pair;

public class ProjectScanner implements AutoCloseable { 
	
	private static final String SCAN_RESULTS_FILE_NAME = "project_scan_result.csv";
	private String migrationBranchName;

	private final Logger logger = Logger.getLogger(ProjectScanner.class.getName());
	
	private final GitLabApi privilegedGitLabApi;

	private Map<String,ProjectReport> projectReportsById;

	private LegacyProjectHandler legacyProjectHandler;
	
	
	public ProjectScanner() throws Exception {
		
		
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		Properties catmaProperties = new Properties();
		catmaProperties.load(new FileInputStream(propertiesFile));

		CATMAProperties.INSTANCE.setProperties(catmaProperties);
		
		privilegedGitLabApi = new GitLabApi(
				 CATMAPropertyKey.GITLAB_SERVER_URL.getValue(),
				 CATMAPropertyKey.GITLAB_ADMIN_PERSONAL_ACCESS_TOKEN.getValue()
		);
		this.legacyProjectHandler = new LegacyProjectHandler(privilegedGitLabApi);
		
		this.projectReportsById = Maps.newHashMap();
		
		this.migrationBranchName = CATMAPropertyKey.V6_REPO_MIGRATION_BRANCH.getValue();
	}
	
	private ProjectReport getProjectReport(String projectId) {
		ProjectReport projectReport = this.projectReportsById.get(projectId);
		if (projectReport == null) {
			projectReport = new ProjectReport(projectId);
			this.projectReportsById.put(projectId, projectReport);
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
			CredentialsProvider credentialsProvider,
			GitLabApi restrictedGitLabApi,
			User user) throws Exception {
		
		try (JGitRepoManager repoManager = userRepoManager) {
			
			logger.info(String.format("Scanning Project %1$s", projectId));

			String rootRepoName = projectId + "_root";
			repoManager.open(
					projectId,
					rootRepoName);
			List<String> resources = repoManager.getSubmodulePaths();
			
			repoManager.detach();
			
			List<String> readableResources = new ArrayList<String>();
			
			for (String resource : resources) {
				
				RBACRole role = this.legacyProjectHandler.getLegacyResourcePermissions(
						restrictedGitLabApi, projectId, resource);
				
				if (role.equals(RBACRole.GUEST)) {
					logger.info(String.format(
						"User %1$s has only Guest role on resource %2$s/%3$s, skipping", 
						repoManager.getUsername(), projectId, resource));
				}
				else {
					readableResources.add(resource);
					
					repoManager.open(projectId, rootRepoName + "/" + resource);
					repoManager.checkout("dev", true);
					
					Status status = repoManager.getStatus();
		
					getProjectReport(projectId).addStatus(resource, status);

					CommitInfo headCommit = repoManager.getHeadCommit();
					if (headCommit != null) {
						getProjectReport(projectId).addLastHeadCommit(headCommit);
					}
					
					if (status.isClean()) {
						logger.info(String.format("Fetching resource %1$s/%2$s", projectId, resource));
						
						repoManager.fetch(credentialsProvider);
						
						
						List<CommitInfo> commitsDevToMaster = 
								repoManager.getCommitsNeedToBeMergedFromDevToMaster();
						getProjectReport(projectId).addNeedMergeDevToMaster(
								resource, commitsDevToMaster);
						
						if (!commitsDevToMaster.isEmpty()) {
							boolean canMergeDevToMaster = 
									repoManager.canMerge(Constants.MASTER);
							
							getProjectReport(projectId).addCanMergeDevToMaster(
								resource, canMergeDevToMaster);
							
						}
						
						List<CommitInfo> commitsMasterToOriginMaster = 
								repoManager.getCommitsNeedToBeMergedFromMasterToOriginMaster();
						getProjectReport(projectId).addNeedMergeMasterToOriginMaster(
								resource, commitsMasterToOriginMaster);
						
						if (!commitsMasterToOriginMaster.isEmpty() || !commitsDevToMaster.isEmpty()) {
							// we are checking dev against origin master here
							// because ultimately that's where all dev commits need to end up without conflicts
							getProjectReport(projectId).addCanMergeDevToOriginMaster(
								resource, 
								repoManager.canMerge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER));
						}
					}
					
					repoManager.detach();
				}
			}
			
			repoManager.open(
					projectId,
					rootRepoName);
			
			Status rootStatus = repoManager.getStatus();
			
			getProjectReport(projectId).addStatus(projectId, rootStatus);
			
			CommitInfo headCommit = repoManager.getHeadCommit();
			if (headCommit != null) {
				getProjectReport(projectId).addLastHeadCommit(headCommit);
			}

			if (rootStatus.isClean()) {
				logger.info(String.format("Fetching root %1$s", projectId));
				
				repoManager.fetch(credentialsProvider);
				List<CommitInfo> commits = repoManager.getCommitsNeedToBeMergedFromMasterToOriginMaster();
				getProjectReport(projectId).addNeedMergeMasterToOriginMaster(
						projectId, commits);
				
				if (!commits.isEmpty()) {
					getProjectReport(projectId).addCanMergeMasterToOriginMaster(
						projectId, 
						repoManager.canMerge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER));
				}
			}

			repoManager.detach();
			
			if (CATMAPropertyKey.V6_REPO_MIGRATION_SCAN_WITH_MERGE_AND_PUSH.getBooleanValue()) {
				for (String resource : readableResources) {
					repoManager.open(projectId, rootRepoName + "/" + resource);
					Status status = repoManager.getStatus();
		
					if (status.isClean()) {
						mergeAndPushResource(repoManager, credentialsProvider, projectId, resource);
					}
					
					repoManager.detach();
				}
				
				 // Did we have a clean rootStatus before merging resources?
				if (rootStatus.isClean()) { 
					repoManager.open(
							projectId,
							rootRepoName);
					mergeAndPushRoot(repoManager, user, credentialsProvider, projectId, rootRepoName);
					repoManager.detach();
				}
			}
		}
		catch (Exception e) {
			logger.log(Level.INFO, String.format("Error scanning Project %1$s", projectId), e);
			getProjectReport(projectId).addError(e);
		}
	}
	
	private boolean resolveRootConflicts(
			JGitRepoManager repoManager, 
			User user, CredentialsProvider credentialsProvider, 
			MergeResult mergeResult, String projectId, String rootRepoName) throws Exception {
		boolean clean = true;
		if (!mergeResult.getMergeStatus().isSuccessful()) {
			if (mergeResult.getConflicts().keySet().contains(Constants.DOT_GIT_MODULES)) {
				logger.info(
						String.format(
								"Found conflicts in %1$s of project %2$s, trying auto resolution",
								Constants.DOT_GIT_MODULES,
								projectId
						)
				);

				repoManager.resolveGitSubmoduleFileConflicts();
			}
			
			clean = repoManager.resolveRootConflicts(projectId, credentialsProvider);
			if (clean) {
				
				List<String> relativeSubmodulePaths = repoManager.getSubmodulePaths();
				for (String relatviveSubmodulePath : relativeSubmodulePaths) {
					Path subModulePath = 
						Paths.get(repoManager.getRepositoryWorkTree().getAbsolutePath(), relatviveSubmodulePath);
					String remoteUrl = String.format("%s/%s/%s.git", CATMAPropertyKey.GITLAB_SERVER_URL.getValue(), projectId, subModulePath.getFileName().toString());
					repoManager.reAddSubmodule(subModulePath.toFile(), remoteUrl, credentialsProvider);
					repoManager.initAndUpdateSubmodules(credentialsProvider, Collections.singleton(relatviveSubmodulePath));
					repoManager.detach();
					repoManager.open(projectId, rootRepoName + "/" + relatviveSubmodulePath);
					repoManager.checkoutFromOrigin(migrationBranchName);
					repoManager.detach();
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

	private void mergeAndPushRoot(JGitRepoManager repoManager, User user, CredentialsProvider credentialsProvider,
			String projectId, String rootRepoName) throws Exception {
		if (repoManager.hasRef(Constants.MASTER)) {
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
			
			repoManager.fetch(credentialsProvider);
			
			boolean hasRemoteC6Migration = repoManager.hasRef(Constants.DEFAULT_REMOTE_NAME + "/" + migrationBranchName);
			boolean clean = true; 
			if (hasRemoteC6Migration) {
				MergeResult mrOriginC6Migration = repoManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + migrationBranchName);
				getProjectReport(projectId).addMergeResultOriginC6MigrationToC6Migration(projectId, mrOriginC6Migration);
				clean = resolveRootConflicts(repoManager, user, credentialsProvider, mrOriginC6Migration, projectId, rootRepoName);
			}
			if (clean) {
				if (repoManager.hasRemoteRef(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER)) {
					MergeResult mrOriginMaster = repoManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER);
					getProjectReport(projectId).addMergeResultOriginMasterToC6Migration(projectId, mrOriginMaster);
					clean = resolveRootConflicts(repoManager, user, credentialsProvider, mrOriginMaster, projectId, rootRepoName);
				}
				if (clean) {
					MergeResult mrMaster = repoManager.merge(Constants.MASTER);
					getProjectReport(projectId).addMergeResulMasterToC6Migration(projectId, mrMaster);
					clean = resolveRootConflicts(repoManager, user, credentialsProvider, mrMaster, projectId, rootRepoName);
					if (clean) {
						repoManager.push(credentialsProvider, true);
						List<CommitInfo> commits = repoManager.getCommitsNeedToBeMergedFromC6MigrationToOriginC6Migration(migrationBranchName);
						if (commits.size() >0) {
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
		else {
			logger.info(String.format("Project %1$s for user %2$s does not have any commits yet!", projectId, user.getIdentifier()));
		}
	}
	private void mergeAndPushResource(JGitRepoManager repoManager, CredentialsProvider credentialsProvider,
			String projectId, String resource) throws Exception {
	
		repoManager.checkout(migrationBranchName, true);
		repoManager.fetch(credentialsProvider);
		
		boolean hasRemoteC6Migration = repoManager.hasRef(Constants.DEFAULT_REMOTE_NAME + "/" + migrationBranchName);
		MergeResult mrOriginC6Migration = null; 
		if (hasRemoteC6Migration) {
			mrOriginC6Migration = repoManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + migrationBranchName);
			getProjectReport(projectId).addMergeResultOriginC6MigrationToC6Migration(resource, mrOriginC6Migration);
		}
		if (!hasRemoteC6Migration || mrOriginC6Migration.getMergeStatus().isSuccessful()) {
			MergeResult mrOriginMaster = repoManager.merge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER);
			getProjectReport(projectId).addMergeResultOriginMasterToC6Migration(resource, mrOriginMaster);
			if (mrOriginMaster.getMergeStatus().isSuccessful()) {
				MergeResult mrDev = repoManager.merge("dev");
				getProjectReport(projectId).addMergeResultDevToC6Migration(resource, mrDev);
				if (mrDev.getMergeStatus().isSuccessful()) {
					repoManager.push(credentialsProvider, true);
					List<CommitInfo> commits = repoManager.getCommitsNeedToBeMergedFromC6MigrationToOriginC6Migration(migrationBranchName);
					if (commits.size() >0) {
						getProjectReport(projectId).addPushC6MigrationToOriginC6MigrationFailed(resource);
					}
					else {
						getProjectReport(projectId).addPushC6MigrationToOriginC6MigrationSuccessful(resource);
					}
				}
			}
		}		
	}

	private void scanProjects(String username, String projectId, String authenticationUsername) {
		logger.info(String.format("Scanning user %1$s Projects: %2$s", username, projectId==null?"all":projectId));
		
		Path userTempPath = null;
		try {
			getProjectReport(projectId).addUser(username);
			
			Pair<User, String> result = this.legacyProjectHandler.aquireUser(username);
			if (result == null) {
				getProjectReport(projectId).addStaleUser(username);
				return;
			}
			
			Pair<User, String> authenticationResult = 
					this.legacyProjectHandler.aquireUser(authenticationUsername);
			
			try (GitLabApi restrictedGitLabApi = new GitLabApi(CATMAPropertyKey.GITLAB_SERVER_URL.getValue(),	authenticationResult.getSecond())) {
				UsernamePasswordCredentialsProvider credentialsProvider = 
					new UsernamePasswordCredentialsProvider(
							"oauth2", restrictedGitLabApi.getAuthToken());
				
				String repositoryPathPath = CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue();
				Path userBasePath = Paths.get(new File(repositoryPathPath).getAbsolutePath(), username);
				
				String repositoryTempPath = new File(CATMAPropertyKey.TEMP_DIR.getValue(), "project_migration").getAbsolutePath();
				userTempPath = Paths.get(repositoryTempPath, username);
				if (userTempPath.toFile().exists()) {
					this.legacyProjectHandler.deleteUserTempPath(userTempPath);
				}
				
				if (!userTempPath.toFile().mkdirs()) {
					throw new IllegalStateException(
						String.format(
							"Could not create temp directory %1$s", 
							userTempPath.toString()));
				}
				
				Path copySrc = userBasePath;
				Path copyDest = userTempPath;
				if (projectId != null) {
					copySrc = copySrc.resolve(projectId);
					copyDest = copyDest.resolve(projectId);
				}
				
				if (copySrc.toFile().exists()) {
					logger.info(String.format("Creating temp directory %1$s...", copyDest.toString()));				
					FileUtils.copyDirectory(copySrc.toFile(), copyDest.toFile());
					logger.info(String.format("Finished creation of temp directory %1$s", copyDest.toString()));
				
					try (JGitRepoManager repoManager = 
							new JGitRepoManager(
									repositoryTempPath, 
									result.getFirst())) {
						
						File[] projectDirs = 
								userTempPath
									.toFile()
									.listFiles(file -> 
										file.isDirectory()
										&& file.getName().startsWith("CATMA"));
						
						List<Group> projects = this.legacyProjectHandler.getLegacyUserProjectReferences(restrictedGitLabApi);
						
						for (File projectDir : projectDirs) {
							if (isValidLegacyProject(projectDir, projects)) {
								scanProject(projectDir.getName(), repoManager, credentialsProvider, restrictedGitLabApi, result.getFirst());
							}
							else {
								logger.info(
									String.format(
										"Found stale Project for user %1$s: %2$s", 
										username, projectDir.getName()));
								getProjectReport(projectId).addStaleProject(projectDir);
							}
						}
					}
				}
				else {
					logger.info(String.format(
							"Project %1$s has not been cloned by user %2$s, skipping!", 
							projectId, username));
				}
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, String.format("Error scanning user %1$s", username), e);
		}
		finally {
			if (userTempPath != null && CATMAPropertyKey.V6_REPO_MIGRATION_REMOVE_USER_TEMP_DIRECTORY.getBooleanValue(true)) {
				logger.info(String.format("Removing temp directory %1$s", userTempPath.toFile()));
				try {
					this.legacyProjectHandler.deleteUserTempPath(userTempPath);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error removing temp file!", e);
				}
			}
		}
		
	}
	
	public void scanProject(String projectId) throws IOException {
		logger.info(String.format("Scanning Project %1$s", projectId));
		
		Set<Member> members = this.legacyProjectHandler.getLegacyProjectMembers(projectId);
		Member owner = members.stream()
				.filter(member -> member.getRole().equals(RBACRole.OWNER))
				.findAny()
				.orElse(null);
		if (owner != null) {
			getProjectReport(projectId).setOwnerEmail(
				owner.getEmail()==null?owner.getIdentifier():owner.getEmail());
		}
		
		this.legacyProjectHandler.removeC6MigrationBranches(projectId, migrationBranchName);
		
		for (Member member : members) {
			// we use the owner for authentication, since merging requires full access to all resources
			scanProjects(member.getIdentifier(), projectId, owner.getIdentifier());
		}
		
		exportProjectReport(getProjectReport(projectId));

		Logger.getLogger(ProjectScanner.class.getName()).info(
				getProjectReport(projectId).toString());
	}
	
	public void scanProjects(int limit) throws Exception {
		logger.info("Scanning Projects");
		
		boolean hasLimit = limit != -1;
		
		Pager<Group> pager = this.legacyProjectHandler.getLegacyProjectReferences();
		
		while(pager.hasNext()) {
			for (Group group : pager.next()) {
				if (hasLimit && limit <= 0) {
					return;
				}
				 // we are interested in Group-based CATMA Projects only
				if (group.getName().startsWith("CATMA")) {
					scanProject(group.getName());
					limit--;
				}
				
			}
		}
	}

	public void scanProjects(String username) {
		scanProjects(username, null, username);
	}
	

	public void scanUsers(int limit) {
		File repositoryBasePath = new File(CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue());
		File[] userDirs = repositoryBasePath.listFiles(file -> file.isDirectory());
		
		if (limit == -1) {
			limit = userDirs.length;
		}
		
		for (File userDir : userDirs) {
			if (limit <= 0) {
				break;
			}
			scanProjects(userDir.getName());
			if (limit%2==0) {
				Logger.getLogger(ProjectScanner.class.getName()).info(
						getSummaryProjectReport().toString());
			}
			limit--;
		}
		
		
	}

	public ProjectReport getSummaryProjectReport() {
		return new ProjectReport(this.projectReportsById.values());
	}

	public void logSummaryProjectReport() {
		logger.info(
				"\n\n\nFinal Project Scan Report\n"+getSummaryProjectReport());
	}
	
	public void exportProjectReport(ProjectReport projectReport) {
		try {

			File scanResultsFile = new File(SCAN_RESULTS_FILE_NAME);
			boolean addHeader = !scanResultsFile.exists();
			
			OutputStreamWriter writer = 
					new OutputStreamWriter(new FileOutputStream(SCAN_RESULTS_FILE_NAME, true), "UTF-8");
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.withDelimiter(';'))) {
            	if (addHeader) {
                	ProjectReport.exportHeaderToCsv(csvPrinter);
            	}
        		projectReport.exportToCsv(csvPrinter);
            	csvPrinter.flush();
            }
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Error exporting scan result!", e);
		}		
	}
	
	
	public void exportSummaryProjectReport() {
		try {

			OutputStreamWriter writer = 
					new OutputStreamWriter(new FileOutputStream(SCAN_RESULTS_FILE_NAME), "UTF-8");
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL.withDelimiter(';'))) {
            	ProjectReport.exportHeaderToCsv(csvPrinter);
            	for (ProjectReport report : this.projectReportsById.values()) {
            		report.exportToCsv(csvPrinter);
            	}
            	csvPrinter.flush();
            }
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, "Error exporting scan result!", e);
		}
	}
	
	@Override
	public void close() throws Exception {
		this.privilegedGitLabApi.close();
	}
	

	
	public static void main(String[] args) throws Exception {
		
		FileHandler fileHandler = new FileHandler("project_scanner.log");
		fileHandler.setFormatter(new SimpleFormatter());
		
		Logger.getLogger("").addHandler(fileHandler);
		
		try (final ProjectScanner projectScanner = new ProjectScanner()) {
			Thread finalReportHook = 
					new Thread() {
		        public void run() {
	    			projectScanner.logSummaryProjectReport();
		        }
		    };
			Runtime.getRuntime().addShutdownHook(finalReportHook);
			
			File exportFile = new File(SCAN_RESULTS_FILE_NAME);
			if (exportFile.exists()) {
				exportFile.delete();
			}
			
			ScanMode scanMode = ScanMode.valueOf(CATMAPropertyKey.V6_REPO_MIGRATION_SCAN_MODE.getValue());
			
			switch (scanMode) {
			case ByUser: {
				String userList = CATMAPropertyKey.V6_REPO_MIGRATION_USER_LIST.getValue();
				
				if ((userList != null) && !userList.isEmpty()) {
					for (String user : userList.split(",")) {
						projectScanner.scanProjects(user.trim());
					}
				}
				else {
					int limit = CATMAPropertyKey.V6_REPO_MIGRATION_MAX_USERS.getIntValue();
					projectScanner.scanUsers(limit);
				}
				
			}
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
			}
			
			projectScanner.logSummaryProjectReport();

			// if we got that far this hook is no longer necessary
			Runtime.getRuntime().removeShutdownHook(finalReportHook);
		}
	}

}
