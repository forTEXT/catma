package de.catma.repository.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.gitlab4j.api.UserApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
//import org.junit.Rule;
//import org.junit.rules.ExpectedException;

import com.google.common.eventbus.EventBus;

import de.catma.backgroundservice.BackgroundService;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.SourceDocumentReference;
import de.catma.document.source.TechInfoSet;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.project.ProjectReference;
import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitProjectsManager;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.repository.git.managers.interfaces.LocalGitRepositoryManager;
import de.catma.util.IDGenerator;
//import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotationTest;

public class GitSourceDocumentHandlerTest {
	private GitlabManagerPrivileged gitlabManagerPrivileged;
	private GitlabManagerRestricted gitlabManagerRestricted;

	private ArrayList<File> directoriesToDeleteOnTearDown = new ArrayList<>();

    public GitSourceDocumentHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

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

		gitlabManagerRestricted = new GitlabManagerRestricted(impersonationToken);
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (directoriesToDeleteOnTearDown.size() > 0) {
			for (File dir : directoriesToDeleteOnTearDown) {
				// files have read-only attribute set on Windows, which we need to clear before the call to `deleteDirectory` will work
				// TODO: this was added before the explicit repository close call was added in JGitRepoManager.close
				//       and can potentially be removed now
				for (Iterator<File> it = FileUtils.iterateFiles(dir, null, true); it.hasNext(); ) {
					File file = it.next();
					file.setWritable(true);
				}

				FileUtils.deleteDirectory(dir);
			}
			directoriesToDeleteOnTearDown.clear();
		}

		// delete the GitLab user that we created in setUp, including associated groups/repos
		UserApi userApi = gitlabManagerPrivileged.getGitLabApi().getUserApi();
		userApi.deleteUser(gitlabManagerRestricted.getUser().getUserId(), true);
//		GitLabServerManagerTest.awaitUserDeleted(userApi, gitlabManagerRestricted.getUser().getUserId());
	}

	@Test
	public void create() throws Exception {
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
			705211438L
		);

		SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo(
			indexInfoSet, contentInfoSet, techInfoSet
		);

		Map<String, List<TermInfo>> terms = new TermExtractor(
				IOUtils.toString(convertedSourceDocumentStream, techInfoSet.getCharset()),
				new ArrayList<>(),
				new ArrayList<>(),
				indexInfoSet.getLocale()
		).getTerms();
		// need to re-instantiate the stream, otherwise an empty file will be written later on (FileInputStream does not support `reset`)
		convertedSourceDocumentStream = new FileInputStream(convertedSourceDocument);

		String sourceDocumentUuid = new IDGenerator().generateDocumentId();
		String tokenizedSourceDocumentFileName = sourceDocumentUuid + "." + "json"; // GraphWorktreeProject.TOKENIZED_FILE_EXTENSION

		/*
		All of the above circumvents file upload, *ContentHandler classes and SourceDocument class
		See GraphWorktreeProject.insert
		 */

		try (LocalGitRepositoryManager jGitRepoManager = new JGitRepoManager(
				CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue(), gitlabManagerRestricted.getUser()
		)) {

			directoriesToDeleteOnTearDown.add(jGitRepoManager.getUserRepositoryBasePath());

			BackgroundService mockBackgroundService = mock(BackgroundService.class);
			EventBus mockEventBus = mock(EventBus.class);

			GitProjectsManager gitProjectManager = new GitProjectsManager(
					CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue(),
					gitlabManagerRestricted,
					(projectId) -> {}, // noop deletion handler
					mockBackgroundService,
					mockEventBus
			);

			ProjectReference projectReference = gitProjectManager.createProject(
				"Test CATMA Project", "This is a test CATMA project"
			);

			// the JGitRepoManager instance should always be in a detached state after GitProjectManager calls return
			assertFalse(jGitRepoManager.isAttached());

			jGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
					jGitRepoManager,
					Paths.get(
							jGitRepoManager.getUserRepositoryBasePath().toString(),
							projectReference.getNamespace(),
							projectReference.getProjectId()
					).toFile(),
					gitlabManagerRestricted.getUsername(),
					gitlabManagerRestricted.getEmail()
			);

			File documentFolder = Paths.get(
					jGitRepoManager.getUserRepositoryBasePath().getPath(),
					projectReference.getNamespace(),
					projectReference.getProjectId(),
					GitProjectHandler.DOCUMENTS_DIRECTORY_NAME,
					sourceDocumentUuid
			).toFile();

			String revisionHash = gitSourceDocumentHandler.create(
					documentFolder,
					sourceDocumentUuid,
					originalSourceDocumentStream, originalSourceDocument.getName(),
					convertedSourceDocumentStream, convertedSourceDocument.getName(),
					terms, tokenizedSourceDocumentFileName,
					sourceDocumentInfo
			);
			assertNotNull(revisionHash);

			assert documentFolder.exists();
			assert documentFolder.isDirectory();
			assert Arrays.asList(documentFolder.list()).contains("rose_for_emily.pdf");
			assert Arrays.asList(documentFolder.list()).contains("rose_for_emily.txt");
			assert FileUtils.contentEquals(
				originalSourceDocument, new File(documentFolder, "rose_for_emily.pdf")
			);
			assert FileUtils.contentEquals(
				convertedSourceDocument, new File(documentFolder, "rose_for_emily.txt")
			);

			assert Arrays.asList(documentFolder.list()).contains("header.json");

			String expectedSerializedSourceDocumentInfo = "" +
					"{\n" +
					"  \"gitContentInfoSet\": {\n" +
					"    \"author\": \"William Faulkner\",\n" +
					"    \"description\": \"\",\n" +
					"    \"publisher\": \"\",\n" +
					"    \"title\": \"A Rose for Emily\"\n" +
					"  },\n" +
					"  \"gitIndexInfoSet\": {\n" +
					"    \"locale\": \"en\",\n" +
					"    \"unseparableCharacterSequences\": [],\n" +
					"    \"userDefinedSeparatingCharacters\": []\n" +
					"  },\n" +
					"  \"gitTechInfoSet\": {\n" +
					"    \"charset\": \"UTF-8\",\n" +
					"    \"checksum\": 705211438,\n" +
					"    \"fileName\": null,\n" +
					"    \"fileOSType\": \"DOS\",\n" +
					"    \"fileType\": \"TEXT\",\n" +
					"    \"mimeType\": \"text/plain\",\n" +
					"    \"responsibleUser\": null\n" +
					"  }\n" +
					"}";

			assertEquals(
				expectedSerializedSourceDocumentInfo,
				FileUtils.readFileToString(new File(documentFolder, "header.json"), StandardCharsets.UTF_8)
			);
		}
	}

//	// how to test for exceptions: https://stackoverflow.com/a/31826781
//	@Rule
//	public ExpectedException thrown = ExpectedException.none();
//
//	@Test
//	public void delete() throws Exception {
//		try (LocalGitRepositoryManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
//			GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
//				jGitRepoManager, this.gitLabServerManager
//			);
//
//			thrown.expect(IOException.class);
//			thrown.expectMessage("Not implemented");
//			gitSourceDocumentHandler.delete("fakeProjectId", "fakeSourceDocumentId");
//		}
//	}
//
//	@Test
//	public void open() throws Exception {
//		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
//			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());
//
//			HashMap<String, Object> getJsonLdWebAnnotationResult = JsonLdWebAnnotationTest.getJsonLdWebAnnotation(
//					jGitRepoManager, this.gitLabServerManager, this.catmaUser
//			);
//
//			String projectId = (String)getJsonLdWebAnnotationResult.get("projectUuid");
//			String sourceDocumentId = (String)getJsonLdWebAnnotationResult.get("sourceDocumentUuid");
//
//			this.projectsToDeleteOnTearDown.add(projectId);
//
//			GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
//					jGitRepoManager, this.gitLabServerManager
//			);
//
//			SourceDocument loadedSourceDocument = gitSourceDocumentHandler.open(projectId, sourceDocumentId);
//
//			assertNotNull(loadedSourceDocument);
//			assertEquals(
//					"William Faulkner",
//					loadedSourceDocument.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet()
//							.getAuthor()
//			);
//			assertEquals(
//					"A Rose for Emily",
//					loadedSourceDocument.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet()
//							.getTitle()
//			);
//			assertNotNull(loadedSourceDocument.getRevisionHash());
//		}
//	}

	@Test
	public void update() throws Exception {
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
				705211438L
		);

		SourceDocumentInfo sourceDocumentInfo = new SourceDocumentInfo(
				indexInfoSet, contentInfoSet, techInfoSet
		);

		Map<String, List<TermInfo>> terms = new TermExtractor(
				IOUtils.toString(convertedSourceDocumentStream, techInfoSet.getCharset()),
				new ArrayList<>(),
				new ArrayList<>(),
				indexInfoSet.getLocale()
		).getTerms();
		// need to re-instantiate the stream, otherwise an empty file will be written later on (FileInputStream does not support `reset`)
		convertedSourceDocumentStream = new FileInputStream(convertedSourceDocument);

		String sourceDocumentUuid = new IDGenerator().generateDocumentId();
		String tokenizedSourceDocumentFileName = sourceDocumentUuid + "." + "json"; // GraphWorktreeProject.TOKENIZED_FILE_EXTENSION

		/*
		All of the above circumvents file upload, *ContentHandler classes and SourceDocument class
		See GraphWorktreeProject.insert
		 */

		try (LocalGitRepositoryManager jGitRepoManager = new JGitRepoManager(
				CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue(), gitlabManagerRestricted.getUser()
		)) {

			directoriesToDeleteOnTearDown.add(jGitRepoManager.getUserRepositoryBasePath());

			BackgroundService mockBackgroundService = mock(BackgroundService.class);
			EventBus mockEventBus = mock(EventBus.class);

			GitProjectsManager gitProjectManager = new GitProjectsManager(
					CATMAPropertyKey.GIT_REPOSITORY_BASE_PATH.getValue(),
					gitlabManagerRestricted,
					(projectId) -> {}, // noop deletion handler
					mockBackgroundService,
					mockEventBus
			);

			ProjectReference projectReference = gitProjectManager.createProject(
					"Test CATMA Project", "This is a test CATMA project"
			);

			// the JGitRepoManager instance should always be in a detached state after GitProjectManager calls return
			assertFalse(jGitRepoManager.isAttached());

			jGitRepoManager.open(projectReference.getNamespace(), projectReference.getProjectId());

			GitSourceDocumentHandler gitSourceDocumentHandler = new GitSourceDocumentHandler(
					jGitRepoManager,
					Paths.get(
							jGitRepoManager.getUserRepositoryBasePath().toString(),
							projectReference.getNamespace(),
							projectReference.getProjectId()
					).toFile(),
					gitlabManagerRestricted.getUsername(),
					gitlabManagerRestricted.getEmail()
			);

			File documentFolder = Paths.get(
					jGitRepoManager.getUserRepositoryBasePath().getPath(),
					projectReference.getNamespace(),
					projectReference.getProjectId(),
					GitProjectHandler.DOCUMENTS_DIRECTORY_NAME,
					sourceDocumentUuid
			).toFile();

			String revisionHash = gitSourceDocumentHandler.create(
					documentFolder,
					sourceDocumentUuid,
					originalSourceDocumentStream, originalSourceDocument.getName(),
					convertedSourceDocumentStream, convertedSourceDocument.getName(),
					terms, tokenizedSourceDocumentFileName,
					sourceDocumentInfo
			);
			assertNotNull(revisionHash);

			// TODO: factor out a function that does all of the above

			SourceDocument sourceDocument = gitSourceDocumentHandler.open(sourceDocumentUuid);
			sourceDocument.getSourceContentHandler().getSourceDocumentInfo().setContentInfoSet(
					new ContentInfoSet(
							"William Faulkner (updated)",
							"Test description (new)",
							"Test publisher (new)",
							"A Rose for Emily (updated)"
					)
			);

			String sourceDocumentRevision = gitSourceDocumentHandler.update(
					new SourceDocumentReference(sourceDocumentUuid, sourceDocument.getSourceContentHandler()));
			assertNotNull(sourceDocumentRevision);

			String expectedSerializedSourceDocumentInfo = "" +
					"{\n" +
					"  \"gitContentInfoSet\": {\n" +
					"    \"author\": \"William Faulkner (updated)\",\n" +
					"    \"description\": \"Test description (new)\",\n" +
					"    \"publisher\": \"Test publisher (new)\",\n" +
					"    \"title\": \"A Rose for Emily (updated)\"\n" +
					"  },\n" +
					"  \"gitIndexInfoSet\": {\n" +
					"    \"locale\": \"en\",\n" +
					"    \"unseparableCharacterSequences\": [],\n" +
					"    \"userDefinedSeparatingCharacters\": []\n" +
					"  },\n" +
					"  \"gitTechInfoSet\": {\n" +
					"    \"charset\": \"UTF-8\",\n" +
					"    \"checksum\": 705211438,\n" +
					"    \"fileName\": null,\n" +
					"    \"fileOSType\": \"DOS\",\n" +
					"    \"fileType\": \"TEXT\",\n" +
					"    \"mimeType\": \"text/plain\",\n" +
					"    \"responsibleUser\": null\n" +
					"  }\n" +
					"}";

			assertEquals(
					expectedSerializedSourceDocumentInfo,
					FileUtils.readFileToString(
							new File(documentFolder, "header.json"),
							StandardCharsets.UTF_8
					)
			);
		}
	}
}
