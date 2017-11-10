package de.catma.repository.git;

import de.catma.document.source.*;
import de.catma.repository.git.managers.GitLabServerManager;
import de.catma.repository.git.managers.GitLabServerManagerTest;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.repository.git.serialization.model_wrappers.GitSourceDocumentInfo;
import helpers.Randomizer;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Status;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.*;

public class ProjectHandlerTest {
	private Properties catmaProperties;
	private de.catma.user.User catmaUser;
	private GitLabServerManager gitLabServerManager;

	private ArrayList<String> projectsToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<File> directoriesToDeleteOnTearDown = new ArrayList<>();

	public ProjectHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
	}

	@Before
	public void setUp() throws Exception {
		// create a fake CATMA user which we'll use to instantiate the GitLabServerManager & JGitRepoManager
		this.catmaUser = Randomizer.getDbUser();

		this.gitLabServerManager = new GitLabServerManager(
			this.catmaProperties, catmaUser
		);
		this.gitLabServerManager.replaceGitLabServerUrl = true;
	}

	@After
	public void tearDown() throws Exception {
		if (this.projectsToDeleteOnTearDown.size() > 0) {
			try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
				ProjectHandler projectHandler = new ProjectHandler(jGitRepoManager, this.gitLabServerManager);

				for (String projectId : this.projectsToDeleteOnTearDown) {
					projectHandler.delete(projectId);
				}
				this.projectsToDeleteOnTearDown.clear();
			}
		}

		if (this.directoriesToDeleteOnTearDown.size() > 0) {
			for (File dir : this.directoriesToDeleteOnTearDown) {
				FileUtils.deleteDirectory(dir);
			}
			this.directoriesToDeleteOnTearDown.clear();
		}

		// delete the GitLab user that the GitLabServerManager constructor in setUp would have
		// created - see GitLabServerManagerTest tearDown() for more info
		User user = this.gitLabServerManager.getGitLabUser();
		this.gitLabServerManager.getAdminGitLabApi().getUserApi().deleteUser(user.getId());
		GitLabServerManagerTest.awaitUserDeleted(
			this.gitLabServerManager.getAdminGitLabApi().getUserApi(), user.getId()
		);
	}

	@Test
	public void create() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			ProjectHandler projectHandler = new ProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = projectHandler.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			assertNotNull(projectId);
			assert projectId.startsWith("CATMA_");

			// the JGitRepoManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());

			String expectedRootRepositoryName = ProjectHandler.getProjectRootRepositoryName(projectId);

			File expectedRootRepositoryPath = new File(
					jGitRepoManager.getRepositoryBasePath(), expectedRootRepositoryName
			);

			assert expectedRootRepositoryPath.exists();
			assert expectedRootRepositoryPath.isDirectory();
		}
	}

	@Test
	public void delete() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			ProjectHandler projectHandler = new ProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = projectHandler.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			// we don't add the projectId to this.projectsToDeleteOnTearDown as this is the delete test

			assertNotNull(projectId);
			assert projectId.startsWith("CATMA_");

			// the JGitRepoManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());

			String expectedRootRepositoryName = ProjectHandler.getProjectRootRepositoryName(projectId);

			File expectedRootRepositoryPath = new File(
					jGitRepoManager.getRepositoryBasePath(), expectedRootRepositoryName
			);

			assert expectedRootRepositoryPath.exists();
			assert expectedRootRepositoryPath.isDirectory();

			projectHandler.delete(projectId);

			assertFalse(expectedRootRepositoryPath.exists());

			// the JGitRepoManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());
		}
	}

	@Test
	public void createTagset() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			ProjectHandler projectHandler = new ProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = projectHandler.create(
					"Test CATMA Project",
					"This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// the JGitRepoManager instance should always be in a detached state after ProjectHandler calls return
			assertFalse(jGitRepoManager.isAttached());

			String tagsetId = projectHandler.createTagset(
					projectId,
					null,
					"Test Tagset",
					null
			);

			assertNotNull(tagsetId);

			// the JGitRepoManager instance should always be in a detached state after ProjectHandler calls return
			assertFalse(jGitRepoManager.isAttached());

			jGitRepoManager.open(ProjectHandler.getProjectRootRepositoryName(projectId));
			Status status = jGitRepoManager.getGitApi().status().call();
			Set<String> added = status.getAdded();

			assert status.hasUncommittedChanges();
			assert added.contains(".gitmodules");
			assert added.contains(String.format("%s/%s", ProjectHandler.TAGSET_SUBMODULES_DIRECTORY_NAME, tagsetId));
		}
	}

	@Test
	public void createMarkupCollection() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			ProjectHandler projectHandler = new ProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = projectHandler.create(
					"Test CATMA Project",
					"This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// the JGitRepoManager instance should always be in a detached state after ProjectHandler calls return
			assertFalse(jGitRepoManager.isAttached());

			String markupCollectionId = projectHandler.createMarkupCollection(
					projectId,
					null,
					"Test Markup Collection",
					null,
					"fakeSourceDocumentId",
					"fakeSourceDocumentVersion"
			);

			assertNotNull(markupCollectionId);

			// the JGitRepoManager instance should always be in a detached state after ProjectHandler calls return
			assertFalse(jGitRepoManager.isAttached());

			jGitRepoManager.open(ProjectHandler.getProjectRootRepositoryName(projectId));
			Status status = jGitRepoManager.getGitApi().status().call();
			Set<String> added = status.getAdded();

			assert status.hasUncommittedChanges();
			assert added.contains(".gitmodules");
			assert added.contains(
					String.format(
							"%s/%s", ProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME, markupCollectionId
					)
			);
		}
	}

	@Test
	public void createSourceDocument() throws Exception {
		File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
		File convertedSourceDocument = new File("testdocs/rose_for_emily.txt");

		FileInputStream originalSourceDocumentStream = new FileInputStream(originalSourceDocument);
		FileInputStream convertedSourceDocumentStream = new FileInputStream(convertedSourceDocument);

		IndexInfoSet indexInfoSet = new IndexInfoSet();
		indexInfoSet.setLocale(Locale.ENGLISH);

		ContentInfoSet contentInfoSet = new ContentInfoSet(
			"William Faulkner",
			"",
			"",
			"A Rose for Emily"
		);

		TechInfoSet techInfoSet = new TechInfoSet(
			FileType.TEXT,
			StandardCharsets.UTF_8,
			FileOSType.DOS,
			705211438L,
			null
		);

		SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo(
			indexInfoSet, contentInfoSet, techInfoSet
		);

		GitSourceDocumentInfo gitSourceDocumentInfo = new GitSourceDocumentInfo(sourceDocumentInfo);

		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			ProjectHandler projectHandler = new ProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = projectHandler.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// the JGitRepoManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());

			String sourceDocumentId = projectHandler.createSourceDocument(
					projectId, null,
					originalSourceDocumentStream, originalSourceDocument.getName(),
					convertedSourceDocumentStream, convertedSourceDocument.getName(),
					gitSourceDocumentInfo
			);

			// the JGitRepoManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());

			jGitRepoManager.open(ProjectHandler.getProjectRootRepositoryName(projectId));
			Status status = jGitRepoManager.getGitApi().status().call();
			Set<String> added = status.getAdded();

			assert status.hasUncommittedChanges();
			assert added.contains(".gitmodules");
			assert added.contains("documents/" + sourceDocumentId);
		}
	}
}
