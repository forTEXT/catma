package de.catma.repository.git;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Status;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.repository.git.managers.GitLabServerManager;
import de.catma.repository.git.managers.GitLabServerManagerTest;
import de.catma.repository.git.managers.JGitRepoManager;
import helpers.Randomizer;
import helpers.UserIdentification;

public class GitProjectHandlerTest {
	private Properties catmaProperties;
	private de.catma.user.User catmaUser;
	private GitLabServerManager gitLabServerManager;

	private ArrayList<String> projectsToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<File> directoriesToDeleteOnTearDown = new ArrayList<>();

	public GitProjectHandlerTest() throws Exception {
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
				this.catmaProperties.getProperty(RepositoryPropertyKey.GitLabServerUrl.name()),
				this.catmaProperties.getProperty(RepositoryPropertyKey.GitLabAdminPersonalAccessToken.name()),
				UserIdentification.userToMap(this.catmaUser.getIdentifier()));	}

	@After
	public void tearDown() throws Exception {
		if (this.projectsToDeleteOnTearDown.size() > 0) {
			try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
				GitProjectHandler gitProjectHandler = new GitProjectHandler(jGitRepoManager, this.gitLabServerManager);

				for (String projectId : this.projectsToDeleteOnTearDown) {
					gitProjectHandler.delete(projectId);
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
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			GitProjectHandler gitProjectHandler = new GitProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = gitProjectHandler.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			assertNotNull(projectId);
			assert projectId.startsWith("CATMA_");

			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());

			String expectedRootRepositoryName = GitProjectHandler.getProjectRootRepositoryName(projectId);

			File expectedRootRepositoryPath = new File(
					jGitRepoManager.getRepositoryBasePath(), expectedRootRepositoryName
			);

			assert expectedRootRepositoryPath.exists();
			assert expectedRootRepositoryPath.isDirectory();
		}
	}

	@Test
	public void delete() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			GitProjectHandler gitProjectHandler = new GitProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = gitProjectHandler.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			// we don't add the projectId to this.projectsToDeleteOnTearDown as this is the delete test

			assertNotNull(projectId);
			assert projectId.startsWith("CATMA_");

			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());

			String expectedRootRepositoryName = GitProjectHandler.getProjectRootRepositoryName(projectId);

			File expectedRootRepositoryPath = new File(
					jGitRepoManager.getRepositoryBasePath(), expectedRootRepositoryName
			);

			assert expectedRootRepositoryPath.exists();
			assert expectedRootRepositoryPath.isDirectory();

			gitProjectHandler.delete(projectId);

			assertFalse(expectedRootRepositoryPath.exists());

			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());
		}
	}

	@Test
	public void createTagset() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			GitProjectHandler gitProjectHandler = new GitProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = gitProjectHandler.create(
					"Test CATMA Project",
					"This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls return
			assertFalse(jGitRepoManager.isAttached());

			String tagsetId = gitProjectHandler.createTagset(
					projectId,
					null,
					"Test Tagset",
					null
			);

			assertNotNull(tagsetId);

			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls return
			assertFalse(jGitRepoManager.isAttached());

			jGitRepoManager.open(projectId, GitProjectHandler.getProjectRootRepositoryName(projectId));
			Status status = jGitRepoManager.getGitApi().status().call();
			Set<String> added = status.getAdded();

			assert status.hasUncommittedChanges();
			assert added.contains(".gitmodules");
			assert added.contains(String.format("%s/%s", GitProjectHandler.TAGSET_SUBMODULES_DIRECTORY_NAME, tagsetId));
		}
	}

	@Test
	public void createMarkupCollection() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			GitProjectHandler gitProjectHandler = new GitProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = gitProjectHandler.create(
					"Test CATMA Project",
					"This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls return
			assertFalse(jGitRepoManager.isAttached());

			String markupCollectionId = gitProjectHandler.createMarkupCollection(
					projectId,
					null,
					"Test Markup Collection",
					null,
					"fakeSourceDocumentId",
					"fakeSourceDocumentVersion"
			);

			assertNotNull(markupCollectionId);

			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls return
			assertFalse(jGitRepoManager.isAttached());

			jGitRepoManager.open(projectId, GitProjectHandler.getProjectRootRepositoryName(projectId));
			Status status = jGitRepoManager.getGitApi().status().call();
			Set<String> added = status.getAdded();

			assert status.hasUncommittedChanges();
			assert added.contains(".gitmodules");
			assert added.contains(
					String.format(
							"%s/%s", GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME, markupCollectionId
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

		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			GitProjectHandler gitProjectHandler = new GitProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = gitProjectHandler.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());

			String sourceDocumentId = gitProjectHandler.createSourceDocument(
					projectId, null,
					originalSourceDocumentStream, originalSourceDocument.getName(),
					convertedSourceDocumentStream, convertedSourceDocument.getName(),
					sourceDocumentInfo
			);

			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());

			jGitRepoManager.open(projectId, GitProjectHandler.getProjectRootRepositoryName(projectId));
			Status status = jGitRepoManager.getGitApi().status().call();
			Set<String> added = status.getAdded();

			assert status.hasUncommittedChanges();
			assert added.contains(".gitmodules");
			assert added.contains(
					String.format(
							"%s/%s",
							GitProjectHandler.SOURCE_DOCUMENT_SUBMODULES_DIRECTORY_NAME,
							sourceDocumentId
					)
			);
		}
	}
}
