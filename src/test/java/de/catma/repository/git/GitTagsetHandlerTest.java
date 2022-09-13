package de.catma.repository.git;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.eventbus.EventBus;

import de.catma.PropertiesHelper;
import de.catma.backgroundservice.BackgroundService;
import de.catma.project.ProjectReference;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

class GitTagsetHandlerTest {

	private static GitlabManagerRestricted gitlabManagerRestricted;
	private static GitlabManagerPrivileged gitlabManagerPrivileged;
	private static ProjectReference projectReference;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		PropertiesHelper.load();
		
		Pair<GitlabManagerRestricted, GitlabManagerPrivileged> result = 
				GitlabTestHelper.createGitlabManagers();
		gitlabManagerRestricted = result.getFirst();
		gitlabManagerPrivileged = result.getSecond();
		
		BackgroundService mockBackgroundService = mock(BackgroundService.class);
		EventBus mockEventBus = mock(EventBus.class);

		GitProjectsManager gitProjectsManager = 
			new GitProjectsManager(
					CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue(),
					gitlabManagerRestricted,
					(projectId) -> {}, // noop deletion handler
					mockBackgroundService,
					mockEventBus
			);

		projectReference = gitProjectsManager.createProject(
			"Test CATMA Project", "This is a test CATMA project"
		);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
//		GitlabTestHelper.deleteUserAndTraces(
//				gitlabManagerRestricted, gitlabManagerPrivileged);
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void createTagset() throws IOException {
		IDGenerator idGenerator = new IDGenerator();
		
		String gitBasedRepositoryBasePath = 
				CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue();
		
		File projectDirectory = 			
			Paths.get(new File(CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue()).toURI())
				.resolve(gitlabManagerRestricted.getUser().getIdentifier())
				.resolve(projectReference.getNamespace())
				.resolve(projectReference.getProjectId())
				.toFile();

		try (ILocalGitRepositoryManager localGitRepositoryManager = 
				new JGitRepoManager(gitBasedRepositoryBasePath, gitlabManagerRestricted.getUser())) {
			
			localGitRepositoryManager.open(projectReference.getNamespace(), projectReference.getProjectId());
			
			String oldProjectRevision = 
					localGitRepositoryManager.getRevisionHash();
			GitTagsetHandler gitTagsetHandler = 
				new GitTagsetHandler(
					localGitRepositoryManager, 
					projectDirectory, 
					gitlabManagerRestricted.getUsername(), gitlabManagerRestricted.getEmail());
			String tagsetId = idGenerator.generate();
			File tagsetFolder =
				Paths.get(projectDirectory.toURI())
					.resolve(GitProjectHandler.TAGSETS_DIRECTORY_NAME)
					.resolve(tagsetId)
					.toFile();
			
			String newProjectRevision = 
				gitTagsetHandler.create(tagsetFolder, tagsetId, "MyTagset", "This is a test Tagset", null);
			
			assert oldProjectRevision != newProjectRevision;
			
			localGitRepositoryManager.push(
				new UsernamePasswordCredentialsProvider(
						"oauth2", gitlabManagerRestricted.getPassword()));

		}
	}

}
