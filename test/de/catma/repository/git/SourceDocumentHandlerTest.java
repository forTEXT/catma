package de.catma.repository.git;

import de.catma.document.source.*;
import de.catma.repository.db.DBUser;
import de.catma.repository.git.exceptions.SourceDocumentHandlerException;
import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManagerTest;
import de.catma.repository.git.model_wrappers.GitSourceDocumentInfo;
import org.apache.commons.io.FileUtils;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

public class SourceDocumentHandlerTest {
	private Properties catmaProperties;
	private RemoteGitServerManager remoteGitServerManager;
	private LocalGitRepositoryManager localGitRepositoryManager;
	private SourceDocumentHandler sourceDocumentHandler;

	private File createdRepositoryPath = null;
	private String insertedSourceDocumentId = null;

    public SourceDocumentHandlerTest() throws Exception {
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

		this.remoteGitServerManager = new RemoteGitServerManager(this.catmaProperties, catmaUser);
		this.remoteGitServerManager.replaceGitLabServerUrl = true;

		this.localGitRepositoryManager = new LocalGitRepositoryManager(this.catmaProperties);

		this.sourceDocumentHandler = new SourceDocumentHandler(
			this.localGitRepositoryManager, this.remoteGitServerManager
		);
	}

	@After
	public void tearDown() throws Exception {
    	if (this.createdRepositoryPath != null) {
			FileUtils.deleteDirectory(this.createdRepositoryPath);
			this.createdRepositoryPath = null;
		}

		if (this.insertedSourceDocumentId != null) {
    		List<Project> projects = this.remoteGitServerManager.getAdminGitLabApi().getProjectApi()
					.getProjects(this.insertedSourceDocumentId);
			for (Project project : projects) {
				this.remoteGitServerManager.deleteRepository(project.getId());
			}
			await().until(
				() -> this.remoteGitServerManager.getAdminGitLabApi().getProjectApi().getProjects()
						.isEmpty()
			);
			this.insertedSourceDocumentId = null;
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
    public void insert() throws Exception {
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

        this.insertedSourceDocumentId = this.sourceDocumentHandler.insert(
			originalSourceDocumentStream, originalSourceDocument.getName(),
			convertedSourceDocumentStream, convertedSourceDocument.getName(),
			gitSourceDocumentInfo,
			null, null
		);

        assertNotNull(this.insertedSourceDocumentId);
        assert this.insertedSourceDocumentId.startsWith("CATMA_");

		File expectedRepoPath = new File(
			this.localGitRepositoryManager.getRepositoryBasePath(), this.insertedSourceDocumentId
		);

		assert expectedRepoPath.exists();
		assert expectedRepoPath.isDirectory();

		this.createdRepositoryPath = expectedRepoPath;

		assert Arrays.asList(expectedRepoPath.list()).contains("rose_for_emily.pdf");
		assert Arrays.asList(expectedRepoPath.list()).contains("rose_for_emily.txt");
		assert FileUtils.contentEquals(
			originalSourceDocument, new File(expectedRepoPath, "rose_for_emily.pdf")
		);
		assert FileUtils.contentEquals(
			convertedSourceDocument, new File(expectedRepoPath, "rose_for_emily.txt")
		);

		assert Arrays.asList(expectedRepoPath.list()).contains("header.json");

		String expectedSerializedSourceDocumentInfo = "" +
				"{\n" +
				"\t\"gitContentInfoSet\":{\n" +
				"\t\t\"author\":\"William Faulkner\",\n" +
				"\t\t\"description\":\"\",\n" +
				"\t\t\"publisher\":\"\",\n" +
				"\t\t\"title\":\"A Rose for Emily\"\n" +
				"\t},\n" +
				"\t\"gitIndexInfoSet\":{\n" +
				"\t\t\"locale\":\"en\",\n" +
				"\t\t\"unseparableCharacterSequences\":[],\n" +
				"\t\t\"userDefinedSeparatingCharacters\":[]\n" +
				"\t},\n" +
				"\t\"gitTechInfoSet\":{\n" +
				"\t\t\"charset\":\"UTF-8\",\n" +
				"\t\t\"checksum\":705211438,\n" +
				"\t\t\"fileName\":null,\n" +
				"\t\t\"fileOSType\":\"INDEPENDENT\",\n" +
				"\t\t\"fileType\":\"TEXT\",\n" +
				"\t\t\"mimeType\":null,\n" +
				"\t\t\"uRI\":null,\n" +
				"\t\t\"xsltDocumentLocalUri\":null\n" +
				"\t}\n" +
				"}";

		assertEquals(
			expectedSerializedSourceDocumentInfo.replaceAll("[\n\t]", ""),
			FileUtils.readFileToString(new File(expectedRepoPath, "header.json"), StandardCharsets.UTF_8)
		);
	}

	@Test
	public void insertIntoProject() throws Exception {
		// we use a separate LocalGitRepositoryManager instance to create the ProjectHandler as the
		// instance that was passed to SourceDocumentHandler in setUp needs to stay unattached
		LocalGitRepositoryManager tempLocalGitRepositoryManager = new LocalGitRepositoryManager(
			this.catmaProperties
		);

		ProjectHandler projectHandler = new ProjectHandler(
			tempLocalGitRepositoryManager, this.remoteGitServerManager
		);

		String projectId = projectHandler.create("Test CATMA Project", null);

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

		this.insertedSourceDocumentId = this.sourceDocumentHandler.insert(
			originalSourceDocumentStream, originalSourceDocument.getName(),
			convertedSourceDocumentStream, convertedSourceDocument.getName(),
			gitSourceDocumentInfo,
			null, projectId
		);

		assertNotNull(this.insertedSourceDocumentId);
		assert this.insertedSourceDocumentId.startsWith("CATMA_");

		File expectedRepoPath = new File(
			this.localGitRepositoryManager.getRepositoryBasePath(), this.insertedSourceDocumentId
		);

		assert expectedRepoPath.exists();
		assert expectedRepoPath.isDirectory();

		this.createdRepositoryPath = expectedRepoPath;

		assert Arrays.asList(expectedRepoPath.list()).contains("rose_for_emily.pdf");
		assert Arrays.asList(expectedRepoPath.list()).contains("rose_for_emily.txt");
		assert FileUtils.contentEquals(
			originalSourceDocument, new File(expectedRepoPath, "rose_for_emily.pdf")
		);
		assert FileUtils.contentEquals(
			convertedSourceDocument, new File(expectedRepoPath, "rose_for_emily.txt")
		);

		assert Arrays.asList(expectedRepoPath.list()).contains("header.json");

		String expectedSerializedSourceDocumentInfo = "" +
				"{\n" +
				"\t\"gitContentInfoSet\":{\n" +
				"\t\t\"author\":\"William Faulkner\",\n" +
				"\t\t\"description\":\"\",\n" +
				"\t\t\"publisher\":\"\",\n" +
				"\t\t\"title\":\"A Rose for Emily\"\n" +
				"\t},\n" +
				"\t\"gitIndexInfoSet\":{\n" +
				"\t\t\"locale\":\"en\",\n" +
				"\t\t\"unseparableCharacterSequences\":[],\n" +
				"\t\t\"userDefinedSeparatingCharacters\":[]\n" +
				"\t},\n" +
				"\t\"gitTechInfoSet\":{\n" +
				"\t\t\"charset\":\"UTF-8\",\n" +
				"\t\t\"checksum\":705211438,\n" +
				"\t\t\"fileName\":null,\n" +
				"\t\t\"fileOSType\":\"INDEPENDENT\",\n" +
				"\t\t\"fileType\":\"TEXT\",\n" +
				"\t\t\"mimeType\":null,\n" +
				"\t\t\"uRI\":null,\n" +
				"\t\t\"xsltDocumentLocalUri\":null\n" +
				"\t}\n" +
				"}";

		assertEquals(
			expectedSerializedSourceDocumentInfo.replaceAll("[\n\t]", ""),
			FileUtils.readFileToString(new File(expectedRepoPath, "header.json"), StandardCharsets.UTF_8)
		);

		// cleanup
		projectHandler.delete(projectId);
		await().until(
			() -> this.remoteGitServerManager.getAdminGitLabApi().getGroupApi().getGroups()
					.isEmpty()
		);
		tempLocalGitRepositoryManager.close();

		// prevent tearDown from also attempting to delete the source document repository (it would
		// have been deleted as part of the project)
		this.insertedSourceDocumentId = null;
	}

	// how to test for exceptions: https://stackoverflow.com/a/31826781
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void remove() throws Exception {
		thrown.expect(SourceDocumentHandlerException.class);
		thrown.expectMessage("Not implemented");
		this.sourceDocumentHandler.remove("fake");
	}
}
