package de.catma.repository.git.migration;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.gitlab4j.api.Constants.ImpersonationState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GroupApi;
import org.gitlab4j.api.UserApi;
import org.gitlab4j.api.models.AccessLevel;
import org.gitlab4j.api.models.Group;
import org.gitlab4j.api.models.GroupFilter;
import org.gitlab4j.api.models.PersonalAccessToken;
import org.gitlab4j.api.models.PersonalAccessToken.Scope;

import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;
import de.catma.rbac.RBACRole;
import de.catma.repository.git.GitUser;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.user.User;
import de.catma.util.Pair;

public class ProjectMigrater  implements AutoCloseable {
	
	private final Logger logger = Logger.getLogger(ProjectMigrater.class.getName());
	
	private final GitLabApi privilegedGitLabApi;

	private MigrationReport migrationReport;
	
	
	public ProjectMigrater() throws Exception {
		
		
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		Properties catmaProperties = new Properties();
		catmaProperties.load(new FileInputStream(propertiesFile));

		CATMAProperties.INSTANCE.setProperties(catmaProperties);
		
		privilegedGitLabApi = new GitLabApi(
				 CATMAPropertyKey.GitLabServerUrl.getValue(), 
				 CATMAPropertyKey.GitLabAdminPersonalAccessToken.getValue()
		);
		
		this.migrationReport = new MigrationReport();
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
	

	private List<Group> getLegacyProjectReferences(GitLabApi restrictedGitLabApi) throws Exception {
		GroupApi groupApi = new GroupApi(restrictedGitLabApi);
		return groupApi.getGroups(new GroupFilter().withMinAccessLevel(AccessLevel.forValue(RBACRole.GUEST.getAccessLevel())));
	}
	

	public void checkProject(
			String projectId, 
			JGitRepoManager userRepoManager, 
			CredentialsProvider credentialsProvider) throws Exception {
		try (JGitRepoManager repoManager = userRepoManager) {
			logger.info(String.format("Checking Project %1$s", projectId));
			String rootRepoName = projectId + "_root";
			repoManager.open(
					projectId,
					rootRepoName);
			
			Status rootStatus = repoManager.getStatus();
			
			this.migrationReport.addStatus(projectId, rootStatus);
			if (rootStatus.isClean()) {
				logger.info(String.format("Fetching root %1$s", projectId));
				
				repoManager.fetch(credentialsProvider);
				this.migrationReport.addUnpushedChanges(projectId, repoManager.getLegacyUnpushedChanges());
			}
			
			List<String> resources = repoManager.getSubmodulePaths();
			
			repoManager.detach();
	
			
			for (String resource : resources) {
				repoManager.open(projectId, rootRepoName + "/" + resource);
				Status status = repoManager.getStatus();
	
				this.migrationReport.addStatus(resource, status);
				
				if (status.isClean()) {
					logger.info(String.format("Fetching resource %1$s/%2$s", projectId, resource));
					
					repoManager.fetch(credentialsProvider);
					this.migrationReport.addUnmergedChanges(resource, repoManager.getLegacyUnmergedChanges());
					this.migrationReport.addUnpushedChanges(resource, repoManager.getLegacyUnpushedChanges());
				}
				
				repoManager.detach();
			}
		}
		catch (Exception e) {
			logger.log(Level.INFO, String.format("Error checking Project %1$s", projectId), e);
			this.migrationReport.addError(projectId);
		}
	}
	
	public void checkProjects(String username) {
		logger.info(String.format("Checking user %1$s", username));
		try {
			Pair<User, String> result = aquireUser(username);
			if (result == null) {
				this.migrationReport.addStaleUser(username);
				return;
			}
			try (GitLabApi restrictedGitLabApi = 
					new GitLabApi(
							CATMAPropertyKey.GitLabServerUrl.getValue(), 
							result.getSecond())) {
				
				UsernamePasswordCredentialsProvider credentialsProvider = 
					new UsernamePasswordCredentialsProvider(
							"oauth2", restrictedGitLabApi.getAuthToken());
				
				try (JGitRepoManager repoManager = 
						new JGitRepoManager(
								CATMAPropertyKey.GitBasedRepositoryBasePath.getValue(), 
								result.getFirst())) {
					
					String repositoryBasePath = 
						new File(CATMAPropertyKey.GitBasedRepositoryBasePath.getValue()).getAbsolutePath();
					Path userBasePath = Paths.get(repositoryBasePath, username);
			
					File[] projectDirs = 
							userBasePath
								.toFile()
								.listFiles(file -> 
									file.isDirectory()
									&& file.getName().startsWith("CATMA"));
					
					List<Group> projects = getLegacyProjectReferences(restrictedGitLabApi);
					
					for (File projectDir : projectDirs) {
						if (isValidLegacyProject(projectDir, projects)) {
							checkProject(projectDir.getName(), repoManager, credentialsProvider);
						}
						else {
							logger.info(
								String.format(
									"Found stale Project for user %1$s: %2$s", 
									username, projectDir.getName()));
							this.migrationReport.addStaleProject(projectDir);
						}
					}
				}
			}
		}
		catch (Exception e) {
			logger.log(Level.SEVERE, String.format("Error checking user %1$s", username), e);
		}
		
	}

	public void checkUsers(int limit) {
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
			checkProjects(userDir.getName());
			limit--;
		}
		
		
	}
	
	private boolean isValidLegacyProject(File projectDir, List<Group> projects) {
		
		for (Group project : projects) {
			if (project.getName().equals(projectDir.getName())) {
				return true;
			}
		}
		
		return false;
	}

	public MigrationReport getMigrationReport() {
		return migrationReport;
	}

	@Override
	public void close() throws Exception {
		this.privilegedGitLabApi.close();
	}
	
	public static void main(String[] args) throws Exception {
		try (ProjectMigrater projectMigrater = new ProjectMigrater()) {	
			String userList = CATMAPropertyKey.Repo6MigrationUserList.getValue();
			if ((userList != null) && !userList.isEmpty()) {
				for (String user : userList.split(",")) {
					projectMigrater.checkProjects(user.trim());
				}
			}
			else {
				int limit = CATMAPropertyKey.Repo6MigrationMaxScannedUsers.getValue(10);
				projectMigrater.checkUsers(limit);
			}
			
			System.out.println(projectMigrater.getMigrationReport());
		}
	}

}
