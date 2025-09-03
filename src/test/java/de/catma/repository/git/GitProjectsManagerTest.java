package de.catma.repository.git;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.eventbus.EventBus;

import de.catma.PropertiesHelper;
import de.catma.backgroundservice.BackgroundService;
import de.catma.project.ProjectReference;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.repository.git.managers.GitProjectsManager;
import de.catma.util.Pair;

class GitProjectsManagerTest {
	
	private GitlabManagerPrivileged gitlabManagerPrivileged;
	private GitlabManagerRestricted gitlabManagerRestricted;

	public GitProjectsManagerTest() throws IOException {
		PropertiesHelper.load();
	}

	@BeforeEach
	public void setUp() throws Exception {
		Pair<GitlabManagerRestricted, GitlabManagerPrivileged> result = 
				GitLabTestHelper.createGitLabManagers();
		this.gitlabManagerRestricted = result.getFirst();
		this.gitlabManagerPrivileged = result.getSecond();
	}
	
	@AfterEach
	public void tearDown() throws Exception {
		GitLabTestHelper.deleteUserAndLocalFiles(gitlabManagerRestricted, gitlabManagerPrivileged);
	}

	@Test
	void createProject() throws Exception {
		BackgroundService mockBackgroundService = mock(BackgroundService.class);
		EventBus mockEventBus = mock(EventBus.class);

		GitProjectsManager gitProjectsManager = new GitProjectsManager(
				CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue(),
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

		File expectedProjectPath = Paths.get(new File(CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue()).toURI())
				.resolve(gitProjectsManager.getUser().getIdentifier())
				.resolve(projectReference.getNamespace())
				.resolve(projectId)
				.toFile();
		assert expectedProjectPath.exists();
		assert expectedProjectPath.isDirectory();

		List<ProjectReference> projectReferences = gitProjectsManager.getProjectReferences();
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
