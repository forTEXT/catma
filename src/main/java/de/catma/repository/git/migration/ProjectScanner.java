package de.catma.repository.git.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.Constants.ImpersonationState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.Pager;
import org.gitlab4j.api.ProjectApi;
import org.gitlab4j.api.UserApi;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.GroupFilter;
import org.gitlab4j.api.models.Permissions;
import org.gitlab4j.api.models.PersonalAccessToken;
import org.gitlab4j.api.models.PersonalAccessToken.Scope;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectAccess;

import de.catma.project.CommitInfo;
import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.GitMember;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.Pair;

public class ProjectScanner  implements AutoCloseable { 
	
	private final Logger logger = Logger.getLogger(ProjectScanner.class.getName());
	
	private final GitLabApi privilegedGitLabApi;

	private ProjectReport projectReport;
	
	
	public ProjectScanner() throws Exception {
		
		
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		Properties catmaProperties = new Properties();
		catmaProperties.load(new FileInputStream(propertiesFile));

		CATMAProperties.INSTANCE.setProperties(catmaProperties);
		
		privilegedGitLabApi = new GitLabApi(
				 CATMAPropertyKey.GitLabServerUrl.getValue(), 
				 CATMAPropertyKey.GitLabAdminPersonalAccessToken.getValue()
		);
		
		this.projectReport = new ProjectReport();
	}
	
	
	private boolean isValidLegacyProject(File projectDir, List<Group> projects) {
		
		for (Group project : projects) {
			if (project.getName().equals(projectDir.getName())) {
				return true;
			}
		}
		
		return false;
	}
	
	private void deleteUserTempPath(Path userTempPath) throws IOException {
		Files.walkFileTree(userTempPath, new SimpleFileVisitor<Path>() {
			public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
				filePath.toFile().setWritable(true, true);
				return FileVisitResult.CONTINUE;
			}
		});
		FileUtils.deleteDirectory(userTempPath.toFile());
	}
	
	private Pair<User, String> aquireUser(String username) throws Exception {
		String tokenName = "migration_admin_token";
		UserApi userApi = this.privilegedGitLabApi.getUserApi();
		
		org.gitlab4j.api.models.User user = userApi.getUser(username);
		
		if (user == null) {
			return null;
		}
		else {
			List<PersonalAccessToken> impersonationTokens = userApi.getImpersonationTokens(
				user.getId(), ImpersonationState.ACTIVE
			);
	
			// revoke the default token if it exists actively
			for (PersonalAccessToken token : impersonationTokens) {
				if (token.getName().equals(tokenName)) {
					privilegedGitLabApi.getUserApi().revokeImpersonationToken(user.getId(), token.getId());
					break;
				}
			}
	
			PersonalAccessToken pat = userApi.createImpersonationToken(
					user.getId(), tokenName, null, new Scope[] {Scope.API}
				);
			
			String impersonationToken = pat.getToken();
			
			return new Pair<User, String>(new GitUser(user), impersonationToken);
		}
	}
	

	private List<Group> getLegacyUserProjectReferences(GitLabApi restrictedGitLabApi) throws Exception {
		GroupApi groupApi = restrictedGitLabApi.getGroupApi();
		return groupApi.getGroups(new GroupFilter().withMinAccessLevel(AccessLevel.forValue(RBACRole.GUEST.getAccessLevel())));
	}
	
	private Pager<Group> getLegacyProjectReferences() throws Exception {
		GroupApi groupApi = privilegedGitLabApi.getGroupApi();
		return groupApi.getGroups(10);
	}	
	
	private RBACRole getLegacyResourcePermissions(GitLabApi restrictedGitLabApi, String projectId, String resource) throws Exception {
		
		String resourceId = resource.substring(resource.lastIndexOf('/')+1);
		
		ProjectApi projectApi = restrictedGitLabApi.getProjectApi();
		Project project = projectApi.getProject(projectId, resourceId);
		Permissions permissions = project.getPermissions();
		
		int level = RBACRole.GUEST.getAccessLevel();
		
		ProjectAccess groupAccess = permissions.getGroupAccess();
		if (groupAccess != null) {
			AccessLevel groupAccessLevel = groupAccess.getAccessLevel();
			if (groupAccessLevel.value.intValue() > level) {
				level = groupAccessLevel.value.intValue();
			}
		}
		
		ProjectAccess projectAccess = permissions.getProjectAccess(); 
		if (projectAccess != null) {
			AccessLevel projectAccessLevel = projectAccess.getAccessLevel();
			if (projectAccessLevel.value.intValue() > level) {
				level = projectAccessLevel.value.intValue();
			}
		}
		return RBACRole.forValue(level);
	}
	
	private Set<Member> getLegacyProjectMembers(String projectId) throws IOException {
		try {
			Group group = privilegedGitLabApi.getGroupApi().getGroup(projectId);
			return privilegedGitLabApi.getGroupApi().getMembers(group.getId())
					.stream()
					.map(member -> new GitMember(member))
					.collect(Collectors.toSet());
		} catch (GitLabApiException e) {
			throw new IOException("group unknown",e);
		}
	}

	private void scanProject(
			String projectId, 
			JGitRepoManager userRepoManager, 
			CredentialsProvider credentialsProvider,
			GitLabApi restrictedGitLabApi) throws Exception {
		
		try (JGitRepoManager repoManager = userRepoManager) {
			
			logger.info(String.format("Scanning Project %1$s", projectId));

			String rootRepoName = projectId + "_root";
			repoManager.open(
					projectId,
					rootRepoName);
			List<String> resources = repoManager.getSubmodulePaths();
			
			repoManager.detach();

			for (String resource : resources) {
				
				RBACRole role = getLegacyResourcePermissions(
						restrictedGitLabApi, projectId, resource);
				
				if (role.equals(RBACRole.GUEST)) {
					logger.info(String.format(
						"User %1$s has only Guest role on resource %2$s/%3$s, skipping", 
						repoManager.getUsername(), projectId, resource));
				}
				else {
					
					repoManager.open(projectId, rootRepoName + "/" + resource);
					Status status = repoManager.getStatus();
		
					this.projectReport.addStatus(projectId, resource, status);
					
					if (status.isClean()) {
						logger.info(String.format("Fetching resource %1$s/%2$s", projectId, resource));
						
						repoManager.fetch(credentialsProvider);
						
						
						List<CommitInfo> commitsDevToMaster = 
								repoManager.getCommitsNeedToBeMergedFromDevToMaster();
						this.projectReport.addNeedMergeDevToMaster(
								projectId, resource, commitsDevToMaster);
						
						if (!commitsDevToMaster.isEmpty()) {
							boolean canMergeDevToMaster = 
									repoManager.canMerge(Constants.MASTER);
							
							this.projectReport.addCanMergeDevToMaster(
								projectId, resource, canMergeDevToMaster);
							
						}
						
						List<CommitInfo> commitsMasterToOriginMaster = 
								repoManager.getCommitsNeedToBeMergedFromMasterToOriginMaster();
						this.projectReport.addNeedMergeMasterToOriginMaster(
								projectId, resource, commitsMasterToOriginMaster);
						
						if (!commitsMasterToOriginMaster.isEmpty() || !commitsDevToMaster.isEmpty()) {
							// we are checking dev against origin master here
							// because ultimately that's where all dev commits need to end up without conflicts
							this.projectReport.addCanMergeDevToOriginMaster(
								projectId,
								resource, 
								repoManager.canMerge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER));
						}
					}
					
					repoManager.detach();
				}
				
				repoManager.open(
						projectId,
						rootRepoName);
				
				Status rootStatus = repoManager.getStatus();
				
				this.projectReport.addStatus(projectId, projectId, rootStatus);
				if (rootStatus.isClean()) {
					logger.info(String.format("Fetching root %1$s", projectId));
					
					repoManager.fetch(credentialsProvider);
					List<CommitInfo> commits = repoManager.getCommitsNeedToBeMergedFromMasterToOriginMaster();
					this.projectReport.addNeedMergeMasterToOriginMaster(
							projectId, projectId, commits);
					
					if (!commits.isEmpty()) {
						this.projectReport.addCanMergeMasterToOriginMaster(
							projectId,
							projectId, 
							repoManager.canMerge(Constants.DEFAULT_REMOTE_NAME + "/" + Constants.MASTER));
					}
				}
				
				repoManager.detach();
			}
		}
		catch (Exception e) {
			logger.log(Level.INFO, String.format("Error scanning Project %1$s", projectId), e);
			this.projectReport.addError(projectId);
		}
	}
	
	private void scanProjects(String username, String projectId) {
		logger.info(String.format("Scanning user %1$s Projects: %2$s", username, projectId==null?"all":projectId));
		
		Path userTempPath = null;
		try {
			this.projectReport.addUser(projectId, username);
			
			Pair<User, String> result = aquireUser(username);
			if (result == null) {
				this.projectReport.addStaleUser(projectId, username);
				return;
			}
			try (GitLabApi restrictedGitLabApi = 
					new GitLabApi(
							CATMAPropertyKey.GitLabServerUrl.getValue(), 
							result.getSecond())) {
				
				UsernamePasswordCredentialsProvider credentialsProvider = 
					new UsernamePasswordCredentialsProvider(
							"oauth2", restrictedGitLabApi.getAuthToken());
				
				String repositoryPathPath = CATMAPropertyKey.GitBasedRepositoryBasePath.getValue();
				Path userBasePath = Paths.get(new File(repositoryPathPath).getAbsolutePath(), username);
				
				String repositoryTempPath = new File(CATMAPropertyKey.TempDir.getValue(), "project_migration").getAbsolutePath();
				userTempPath = Paths.get(repositoryTempPath, username);
				if (userTempPath.toFile().exists()) {
					deleteUserTempPath(userTempPath);
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
					
					List<Group> projects = getLegacyUserProjectReferences(restrictedGitLabApi);
					
					for (File projectDir : projectDirs) {
						if (isValidLegacyProject(projectDir, projects)) {
							scanProject(projectDir.getName(), repoManager, credentialsProvider, restrictedGitLabApi);
						}
						else {
							logger.info(
								String.format(
									"Found stale Project for user %1$s: %2$s", 
									username, projectDir.getName()));
							this.projectReport.addStaleProject(projectDir);
						}
					}
				}
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, String.format("Error scanning user %1$s", username), e);
		}
		finally {
			if (userTempPath != null) {
				logger.info(String.format("Removing temp directory %1$s", userTempPath.toFile()));
				try {
					deleteUserTempPath(userTempPath);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error removing temp file!", e);
				}
			}
		}
		
	}
	
	public void scanProject(String projectId) throws IOException {
		logger.info(String.format("Scanning Project %1$s", projectId));
		
		Set<Member> members = getLegacyProjectMembers(projectId);
		
		for (Member member : members) {
			scanProjects(member.getIdentifier(), projectId);
		}
	}
	
	public void scanProjects(int limit) throws Exception {
		logger.info("Scanning Projects");
		
		boolean hasLimit = limit != -1;
		
		Pager<Group> pager = getLegacyProjectReferences();
		
		while(pager.hasNext()) {
			for (Group group : pager.next()) {
				if (hasLimit && limit <= 0) {
					return;
				}
				
				scanProject(group.getName());
				limit--;
				
			}
		}
	}

	public void scanProjects(String username) {
		scanProjects(username, null);
	}
	

	public void scanUsers(int limit) {
		File repositoryBasePath = 
				new File(CATMAPropertyKey.GitBasedRepositoryBasePath.getValue());

		
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
						getMigrationReport().toString());
			}
			limit--;
		}
		
		
	}

	public ProjectReport getMigrationReport() {
		return projectReport;
	}

	@Override
	public void close() throws Exception {
		this.privilegedGitLabApi.close();
	}
	
	public static void main(String[] args) throws Exception {
		FileHandler fileHandler = new FileHandler("project_scanner.log");
		fileHandler.setFormatter(new SimpleFormatter());
		
		Logger.getLogger("").addHandler(fileHandler);
		
		try (ProjectScanner projectScanner = new ProjectScanner()) {	
			ScanMode scanMode = 
				ScanMode.valueOf(
						CATMAPropertyKey.Repo6MigrationScanMode.getValue(
								CATMAPropertyKey.Repo6MigrationScanMode.getDefaultValue()));
			
			switch (scanMode) {
			case ByUser: {
				String userList = CATMAPropertyKey.Repo6MigrationUserList.getValue();
				
				if ((userList != null) && !userList.isEmpty()) {
					for (String user : userList.split(",")) {
						projectScanner.scanProjects(user.trim());
					}
				}
				else {
					int limit = CATMAPropertyKey.Repo6MigrationMaxScannedUsers.getValue(1);
					projectScanner.scanUsers(limit);
				}
				
			}
			case ByProject:
				String projectList = CATMAPropertyKey.Repo6MigrationProjectIdList.getValue();
				
				if ((projectList != null) && !projectList.isEmpty()) {
					for (String projectId : projectList.split(",")) {
						projectScanner.scanProject(projectId);
					}
				}
				else {
					int limit = CATMAPropertyKey.Repo6MigrationMaxScannedProjects.getValue(1);
					projectScanner.scanProjects(limit);
				}

				Logger.getLogger(ProjectScanner.class.getName()).info(
						"\n\n\nFinal Project Scan Report"+projectScanner.getMigrationReport());

			}
		}
	}

}
