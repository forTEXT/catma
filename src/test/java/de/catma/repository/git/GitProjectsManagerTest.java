package de.catma.repository.git;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.gitlab4j.api.UserApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.eventbus.EventBus;

import de.catma.backgroundservice.BackgroundService;
import de.catma.project.ProjectReference;
import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerRestricted;

class GitProjectsManagerTest {
	
	private GitlabManagerPrivileged gitlabManagerPrivileged;
	private GitlabManagerRestricted gitlabManagerRestricted;


	public GitProjectsManagerTest() throws FileNotFoundException, IOException {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";
	    System.out.println(new File(propertiesFile).getAbsolutePath());
		Properties catmaProperties = new Properties();
		catmaProperties.load(new FileInputStream(propertiesFile));
		CATMAProperties.INSTANCE.setProperties(catmaProperties);
	}

	
	@BeforeEach
	public void setUp() throws Exception {
		// create a fake CATMA user which we'll use to instantiate GitlabManagerRestricted (using the corresponding impersonation token) & JGitRepoManager
		Integer randomUserId = Integer.parseInt(RandomStringUtils.randomNumeric(3));
		String username = String.format("testuser-%s", randomUserId);
		String email = String.format("%s@catma.de", username);
		String name = String.format("Test User %s", randomUserId);

		gitlabManagerPrivileged = new GitlabManagerPrivileged();
		String impersonationToken = gitlabManagerPrivileged.acquireImpersonationToken(username, "catma", email, name).getSecond();

		EventBus mockEventBus = mock(EventBus.class);
		BackgroundService mockBackgroundService = mock(BackgroundService.class);
		gitlabManagerRestricted = new GitlabManagerRestricted(mockEventBus, mockBackgroundService, impersonationToken);
	}
	
	@AfterEach
	public void tearDown() throws Exception {
//		UserApi userApi = gitlabManagerPrivileged.getGitLabApi().getUserApi();
//		userApi.deleteUser(gitlabManagerRestricted.getUser().getUserId(), true);
//		FileUtils.deleteDirectory(
//				Paths.get(new File(CATMAPropertyKey.GitBasedRepositoryBasePath.getValue()).toURI())
//				.resolve(gitlabManagerRestricted.getUser().getIdentifier())
//				.toFile());
	}

	@Test
	void createProject() throws Exception {
		BackgroundService mockBackgroundService = mock(BackgroundService.class);
		EventBus mockEventBus = mock(EventBus.class);

		GitProjectsManager gitProjectsManager = 
			new GitProjectsManager(
					CATMAPropertyKey.GitBasedRepositoryBasePath.getValue(),
					gitlabManagerRestricted,
					(projectId) -> {}, // noop deletion handler
					mockBackgroundService,
					mockEventBus
			);

		ProjectReference projectReference = gitProjectsManager.createProject(
			"Test CATMA Project", "This is a test CATMA project"
		);

		String projectId = projectReference.getProjectId();
		
		assertNotNull(projectId);
		assert projectId.startsWith("CATMA_");


		File expectedProjectPath = 
			Paths.get(new File(CATMAPropertyKey.GitBasedRepositoryBasePath.getValue()).toURI())
				.resolve(gitProjectsManager.getUser().getIdentifier())
				.resolve(projectReference.getNamespace())
				.resolve(projectId)
				.toFile();

		assert expectedProjectPath.exists();
		assert expectedProjectPath.isDirectory();
		
		
		List<ProjectReference> projectReferences = 
				gitProjectsManager.getProjectReferences();

		assert projectReferences.contains(projectReference);
	}
	
//	@Test
//	void getProjectList() throws Exception {
//		String impersonationToken = gitlabManagerPrivileged.acquireImpersonationToken(
//				"testuser-159","catma", "testuser-159@catma.de", 
//				"Test User 159").getSecond();
//
//		BackgroundService mockBackgroundService = mock(BackgroundService.class);
//		EventBus mockEventBus = mock(EventBus.class);
//		gitlabManagerRestricted = 
//				new GitlabManagerRestricted(mockEventBus, mockBackgroundService, impersonationToken);
//
//		GitProjectsManager gitProjectsManager = 
//			new GitProjectsManager(
//					CATMAPropertyKey.GitBasedRepositoryBasePath.getValue(),
//					gitlabManagerRestricted,
//					(projectId) -> {}, // noop deletion handler
//					mockBackgroundService,
//					mockEventBus
//			);
//
//		List<ProjectReference> projectReferences = 
//				gitProjectsManager.getProjectReferences();
//		
//		System.out.println(projectReferences); //TODO: check and create/delete
//	}
}
