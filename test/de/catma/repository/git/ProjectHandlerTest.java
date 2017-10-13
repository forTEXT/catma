package de.catma.repository.git;

import de.catma.document.source.*;
import de.catma.repository.db.DBUser;
import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManagerTest;
import de.catma.repository.git.model_wrappers.GitSourceDocumentInfo;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Status;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;

public class ProjectHandlerTest {
	private Properties catmaProperties;
	private RemoteGitServerManager remoteGitServerManager;
	private LocalGitRepositoryManager localGitRepositoryManager;
	private ProjectHandler projectHandler;

	private String createdProjectId = null;

	public ProjectHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
	}

	@Before
	public void setUp() throws Exception {
		// create a fake CATMA user which we'll use to instantiate the RemoteGitServerManager
		de.catma.user.User catmaUser = new DBUser(
			1, "catma-testuser", false, false, false
		);

		this.remoteGitServerManager = new RemoteGitServerManager(
			this.catmaProperties, catmaUser
		);
		this.remoteGitServerManager.replaceGitLabServerUrl = true;

		this.localGitRepositoryManager = new LocalGitRepositoryManager(this.catmaProperties);

		this.projectHandler = new ProjectHandler(
			this.localGitRepositoryManager,
			this.remoteGitServerManager
		);
	}

	@After
	public void tearDown() throws Exception {
		if (this.createdProjectId != null) {
			this.projectHandler.delete(this.createdProjectId);
			this.createdProjectId = null;
		}

		if (this.localGitRepositoryManager != null) {
			this.localGitRepositoryManager.close();
			this.localGitRepositoryManager = null;
		}

		// delete the GitLab user that the RemoteGitServerManager constructor in setUp would have
		// created - see RemoteGitServerManagerTest tearDown() for more info
		User user = this.remoteGitServerManager.getGitLabUser();
		this.remoteGitServerManager.getAdminGitLabApi().getUserApi().deleteUser(user.getId());
		RemoteGitServerManagerTest.awaitUserDeleted(
			this.remoteGitServerManager.getAdminGitLabApi().getUserApi(), user.getId()
		);
	}

	@Test
	public void create() throws Exception {
		this.createdProjectId = this.projectHandler.create(
			"Test CATMA Project", "This is a test CATMA project"
		);

		assertNotNull(this.createdProjectId);
		assert this.createdProjectId.startsWith("CATMA_");

		String expectedRootRepositoryName = String.format(
			ProjectHandler.PROJECT_ROOT_REPOSITORY_NAME_FORMAT, this.createdProjectId
		);
		String repositoryBasePath = catmaProperties.getProperty("GitBasedRepositoryBasePath");

		File expectedRootRepositoryPath = new File(repositoryBasePath, expectedRootRepositoryName);

		assert expectedRootRepositoryPath.exists();
		assert expectedRootRepositoryPath.isDirectory();

		assert Arrays.asList(expectedRootRepositoryPath.list()).contains("tagsets.json");
		assertEquals(
			"", FileUtils.readFileToString(
					new File(expectedRootRepositoryPath, "tagsets.json"), StandardCharsets.UTF_8)
		);

		assert Arrays.asList(expectedRootRepositoryPath.list()).contains("collections.json");
		assertEquals(
			"", FileUtils.readFileToString(
					new File(expectedRootRepositoryPath, "collections.json"), StandardCharsets.UTF_8)
		);

		assert Arrays.asList(expectedRootRepositoryPath.list()).contains("documents.json");
		assertEquals(
			"", FileUtils.readFileToString(
					new File(expectedRootRepositoryPath, "documents.json"), StandardCharsets.UTF_8)
		);
	}

	@Test
	public void delete() throws Exception {
		this.createdProjectId = this.projectHandler.create(
			"Test CATMA Project", "This is a test CATMA project"
		);

		assertNotNull(this.createdProjectId);
		assert this.createdProjectId.startsWith("CATMA_");

		String expectedRootRepositoryName = String.format(
			ProjectHandler.PROJECT_ROOT_REPOSITORY_NAME_FORMAT, this.createdProjectId
		);
		String repositoryBasePath = catmaProperties.getProperty("GitBasedRepositoryBasePath");

		File expectedRootRepositoryPath = new File(repositoryBasePath, expectedRootRepositoryName);

		assert expectedRootRepositoryPath.exists();
		assert expectedRootRepositoryPath.isDirectory();

		this.projectHandler.delete(this.createdProjectId);

		assertFalse(expectedRootRepositoryPath.exists());

		// prevent tearDown from also attempting to delete the project
		this.createdProjectId = null;
	}

	@Test
	public void insertSourceDocument() throws Exception {
		this.createdProjectId = this.projectHandler.create(
			"Test CATMA Project", "This is a test CATMA project"
		);

		assertFalse(this.localGitRepositoryManager.isAttached());

		File originalSourceDocument = new File("testdocs/rose_for_emily.pdf");
		File convertedSourceDocument = new File("testdocs/rose_for_emily.txt");

		FileInputStream originalSourceDocumentStream = new FileInputStream(originalSourceDocument);
		FileInputStream convertedSourceDocumentStream = new FileInputStream(
			convertedSourceDocument
		);

		IndexInfoSet indexInfoSet = new IndexInfoSet();
		indexInfoSet.setLocale(Locale.ENGLISH);

		ContentInfoSet contentInfoSet = new ContentInfoSet(
			"William Faulkner",
			"",
			"",
			"A Rose for Emily"
		);

		// TODO: should the TechInfoSet represent the original or the converted source document?
		TechInfoSet techInfoSet = new TechInfoSet(
			FileType.TEXT,
			StandardCharsets.UTF_8,
			FileOSType.INDEPENDENT,
			705211438L,
			null
		);

		SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo(
			indexInfoSet, contentInfoSet, techInfoSet
		);

		GitSourceDocumentInfo gitSourceDocumentInfo = new GitSourceDocumentInfo(sourceDocumentInfo);

		String sourceDocumentId = this.projectHandler.insertSourceDocument(
			this.createdProjectId,
			originalSourceDocumentStream, originalSourceDocument.getName(),
			convertedSourceDocumentStream, convertedSourceDocument.getName(),
			gitSourceDocumentInfo, null
		);

		this.localGitRepositoryManager.open(
			String.format(ProjectHandler.PROJECT_ROOT_REPOSITORY_NAME_FORMAT, this.createdProjectId)
		);
		Status status = this.localGitRepositoryManager.getGitApi().status().call();
		Set<String> added = status.getAdded();

		assert status.hasUncommittedChanges();
		assert added.contains(".gitmodules");
		assert added.contains("documents/" + sourceDocumentId);
	}
}
