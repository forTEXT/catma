package de.catma.repository.git;

import de.catma.document.source.*;
import de.catma.repository.db.DBUser;
import de.catma.repository.git.exceptions.SourceDocumentHandlerException;
import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManagerTest;
import de.catma.repository.git.model_wrappers.GitSourceDocumentInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.util.*;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

public class SourceDocumentHandlerTest {
	private Properties catmaProperties;
	private RemoteGitServerManager remoteGitServerManager;

	private ArrayList<File> directoriesToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<String> sourceDocumentReposToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<String> projectsToDeleteOnTearDown = new ArrayList<>();

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
			1, String.format("catma-testuser-%s", RandomStringUtils.randomAlphanumeric(3)),
			false, false, false
		);

		this.remoteGitServerManager = new RemoteGitServerManager(this.catmaProperties, catmaUser);
		this.remoteGitServerManager.replaceGitLabServerUrl = true;
	}

	@After
	public void tearDown() throws Exception {
		if (this.directoriesToDeleteOnTearDown.size() > 0) {
			for (File dir : this.directoriesToDeleteOnTearDown) {
				FileUtils.deleteDirectory(dir);
			}
			this.directoriesToDeleteOnTearDown.clear();
		}

		if (this.sourceDocumentReposToDeleteOnTearDown.size() > 0) {
			for (String sourceDocumentId : this.sourceDocumentReposToDeleteOnTearDown) {
				List<Project> projects = this.remoteGitServerManager.getAdminGitLabApi().getProjectApi().getProjects(
					sourceDocumentId
				); // this getProjects overload does a search
				for (Project project : projects) {
					this.remoteGitServerManager.deleteRepository(project.getId());
				}
				await().until(
					() -> this.remoteGitServerManager.getAdminGitLabApi().getProjectApi().getProjects().isEmpty()
				);
			}
			this.sourceDocumentReposToDeleteOnTearDown.clear();
		}

		if (this.projectsToDeleteOnTearDown.size() > 0) {
			try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
				ProjectHandler projectHandler = new ProjectHandler(localGitRepoManager, this.remoteGitServerManager);

				for (String projectId : this.projectsToDeleteOnTearDown) {
					projectHandler.delete(projectId);
				}
				this.projectsToDeleteOnTearDown.clear();
			}
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
		FileInputStream convertedSourceDocumentStream = new FileInputStream(convertedSourceDocument);

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

		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler(
				localGitRepoManager, this.remoteGitServerManager
			);

			String sourceDocumentId = sourceDocumentHandler.insert(
				originalSourceDocumentStream, originalSourceDocument.getName(),
				convertedSourceDocumentStream, convertedSourceDocument.getName(),
				gitSourceDocumentInfo,
				null, null
			);
			this.sourceDocumentReposToDeleteOnTearDown.add(sourceDocumentId);

			assertNotNull(sourceDocumentId);
			assert sourceDocumentId.startsWith("CATMA_");

			// the LocalGitRepositoryManager instance should always be in a detached state after SourceDocumentHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			File expectedRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), sourceDocumentId);
			assert expectedRepoPath.exists();
			assert expectedRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(expectedRepoPath);
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
	}

	@Test
	public void insertIntoProject() throws Exception {
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

		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler(
				localGitRepoManager, this.remoteGitServerManager
			);

			ProjectHandler projectHandler = new ProjectHandler(
				localGitRepoManager, this.remoteGitServerManager
			);

			String projectId = projectHandler.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// the LocalGitRepositoryManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(localGitRepoManager.isAttached());

			String sourceDocumentId = sourceDocumentHandler.insert(
				originalSourceDocumentStream, originalSourceDocument.getName(),
				convertedSourceDocumentStream, convertedSourceDocument.getName(),
				gitSourceDocumentInfo,
				null, projectId
			);
			// we don't add the sourceDocumentId to this.sourceDocumentReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			assertNotNull(sourceDocumentId);
			assert sourceDocumentId.startsWith("CATMA_");

			// the LocalGitRepositoryManager instance should always be in a detached state after SourceDocumentHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			File expectedRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), sourceDocumentId);

			assert expectedRepoPath.exists();
			assert expectedRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(expectedRepoPath);
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
	}

	// how to test for exceptions: https://stackoverflow.com/a/31826781
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void remove() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			SourceDocumentHandler sourceDocumentHandler = new SourceDocumentHandler(
				localGitRepoManager, this.remoteGitServerManager
			);

			thrown.expect(SourceDocumentHandlerException.class);
			thrown.expectMessage("Not implemented");
			sourceDocumentHandler.remove("fake");
		}
	}
}
