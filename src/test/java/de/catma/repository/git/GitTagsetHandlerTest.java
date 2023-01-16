package de.catma.repository.git;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

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
import de.catma.repository.git.managers.*;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.util.IDGenerator;
import de.catma.util.Pair;

class GitTagsetHandlerTest {

	private static GitlabManagerRestricted gitlabManagerRestricted;
	private static GitlabManagerPrivileged gitlabManagerPrivileged;
	private static ProjectReference projectReference;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		PropertiesHelper.load();

		Pair<GitlabManagerRestricted, GitlabManagerPrivileged> result = GitLabTestHelper.createGitLabManagers();
		gitlabManagerRestricted = result.getFirst();
		gitlabManagerPrivileged = result.getSecond();

		BackgroundService mockBackgroundService = mock(BackgroundService.class);
		EventBus mockEventBus = mock(EventBus.class);

		GitProjectsManager gitProjectsManager = new GitProjectsManager(
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
		String gitRepositoryBasePath = CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue();

		File projectDirectory = Paths.get(new File(gitRepositoryBasePath).toURI())
				.resolve(gitlabManagerRestricted.getUser().getIdentifier())
				.resolve(projectReference.getNamespace())
				.resolve(projectReference.getProjectId())
				.toFile();

		try (LocalGitRepositoryManager localGitRepositoryManager =
				new JGitRepoManager(gitRepositoryBasePath, gitlabManagerRestricted.getUser())
		) {

			localGitRepositoryManager.open(projectReference.getNamespace(), projectReference.getProjectId());
			String oldProjectRevision = localGitRepositoryManager.getRevisionHash();

			GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(
					localGitRepositoryManager,
					projectDirectory, 
					gitlabManagerRestricted.getUsername(), gitlabManagerRestricted.getEmail()
			);

			String tagsetId = new IDGenerator().generate();
			File tagsetDirectory = Paths.get(projectDirectory.toURI())
					.resolve(GitProjectHandler.TAGSETS_DIRECTORY_NAME)
					.resolve(tagsetId)
					.toFile();

			String newProjectRevision = gitTagsetHandler.create(
					tagsetDirectory,
					tagsetId,
					"MyTagset",
					"This is a test tagset",
					null
			);
			assert !newProjectRevision.equals(oldProjectRevision);

			localGitRepositoryManager.push(new JGitCredentialsManager(gitlabManagerRestricted));
		}
	}
}
