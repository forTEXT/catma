package de.catma.repository.git;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import com.google.common.eventbus.EventBus;
import de.catma.backgroundservice.BackgroundService;
import de.catma.indexer.TermExtractor;
import de.catma.indexer.TermInfo;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.util.IDGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.jgit.api.Status;
import org.gitlab4j.api.UserApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.catma.properties.CATMAProperties;
import de.catma.properties.CATMAPropertyKey;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.managers.GitLabServerManagerTest;
import de.catma.repository.git.managers.JGitRepoManager;

public class GitProjectHandlerTest {
	private GitlabManagerPrivileged gitlabManagerPrivileged;
	private GitlabManagerRestricted gitlabManagerRestricted;

	private ArrayList<String> projectsToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<File> directoriesToDeleteOnTearDown = new ArrayList<>();

	public GitProjectHandlerTest() throws Exception {
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

		EventBus mockEventBus = mock(EventBus.class);
		BackgroundService mockBackgroundService = mock(BackgroundService.class);
		gitlabManagerRestricted = new GitlabManagerRestricted(mockEventBus, mockBackgroundService, impersonationToken);
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (projectsToDeleteOnTearDown.size() > 0) {
			BackgroundService mockBackgroundService = mock(BackgroundService.class);
			EventBus mockEventBus = mock(EventBus.class);

			GitProjectManager gitProjectManager = new GitProjectManager(
					CATMAPropertyKey.GitBasedRepositoryBasePath.getValue(),
					gitlabManagerRestricted,
					(projectId) -> {}, // noop deletion handler
					mockBackgroundService,
					mockEventBus
			);

			for (String projectId : projectsToDeleteOnTearDown) {
				gitProjectManager.delete(projectId);
			}
			projectsToDeleteOnTearDown.clear();
		}

		if (directoriesToDeleteOnTearDown.size() > 0) {
			for (File dir : directoriesToDeleteOnTearDown) {
				// files have read-only attribute set on Windows, which we need to clear before the call to `deleteDirectory` will work
				for (Iterator<File> it = FileUtils.iterateFiles(dir, null, true); it.hasNext(); ) {
					File file = it.next();
					file.setWritable(true);
				}

				FileUtils.deleteDirectory(dir);
			}
			directoriesToDeleteOnTearDown.clear();
		}

		// delete the GitLab user that we created in setUp, including associated groups/repos
		// TODO: explicit deletion of associated repos (above) is now superfluous since we are doing a hard delete
		UserApi userApi = gitlabManagerPrivileged.getGitLabApi().getUserApi();
		userApi.deleteUser(gitlabManagerRestricted.getUser().getUserId(), true);
//		GitLabServerManagerTest.awaitUserDeleted(userApi, gitlabManagerRestricted.getUser().getUserId());
	}

	@Test
	public void create() throws Exception {
		try (ILocalGitRepositoryManager jGitRepoManager = new JGitRepoManager(
				CATMAPropertyKey.GitBasedRepositoryBasePath.getValue(), gitlabManagerRestricted.getUser()
		)) {

			directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			BackgroundService mockBackgroundService = mock(BackgroundService.class);
			EventBus mockEventBus = mock(EventBus.class);

			GitProjectManager gitProjectManager = new GitProjectManager(
					CATMAPropertyKey.GitBasedRepositoryBasePath.getValue(),
					gitlabManagerRestricted,
					(projectId) -> {}, // noop deletion handler
					mockBackgroundService,
					mockEventBus
			);

			String projectId = gitProjectManager.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			// we don't add the projectId to projectsToDeleteOnTearDown as deletion of the user will take care of that for us

			assertNotNull(projectId);
			assert projectId.startsWith("CATMA_");

			// the JGitRepoManager instance should always be in a detached state after GitProjectManager calls return
			assertFalse(jGitRepoManager.isAttached());

			String expectedRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

			File expectedRootRepositoryPath = Paths.get(jGitRepoManager.getRepositoryBasePath().getPath(), projectId, expectedRootRepositoryName).toFile();

			assert expectedRootRepositoryPath.exists();
			assert expectedRootRepositoryPath.isDirectory();
		}
	}

//	@Test
//	public void delete() throws Exception {
//		try (ILocalGitRepositoryManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(CATMAPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
//			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());
//
//			GitProjectManager gitProjectHandler = new GitProjectManager(
//					CATMAPropertyKey.GitBasedRepositoryBasePath.getValue(),
//					UserIdentification.userToMap(this.catmaUser.getIdentifier()));
//
//
//			String projectId = gitProjectHandler.create(
//				"Test CATMA Project", "This is a test CATMA project"
//			);
//			// we don't add the projectId to this.projectsToDeleteOnTearDown as this is the delete test
//
//			assertNotNull(projectId);
//			assert projectId.startsWith("CATMA_");
//
//			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls
//			// return
//			assertFalse(jGitRepoManager.isAttached());
//
//			String expectedRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
//
//			File expectedRootRepositoryPath = new File(
//					jGitRepoManager.getRepositoryBasePath(), expectedRootRepositoryName
//			);
//
//			assert expectedRootRepositoryPath.exists();
//			assert expectedRootRepositoryPath.isDirectory();
//
//			gitProjectHandler.delete(projectId);
//
//			assertFalse(expectedRootRepositoryPath.exists());
//
//			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls
//			// return
//			assertFalse(jGitRepoManager.isAttached());
//		}
//	}
//
//	@Test
//	public void createTagset() throws Exception {
//		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(CATMAPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
//			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());
//
//			GitProjectManager gitProjectManager = new GitProjectManager(
//					CATMAPropertyKey.GitBasedRepositoryBasePath.getValue(),
//					UserIdentification.userToMap(this.catmaUser.getIdentifier()));
//
//
//			String projectId = gitProjectManager.create(
//					"Test CATMA Project",
//					"This is a test CATMA project"
//			);
//			this.projectsToDeleteOnTearDown.add(projectId);
//
//			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls return
//			assertFalse(jGitRepoManager.isAttached());
//
//			GitProjectHandler gitProjectHandler = new GitProjectHandler(null, projectId, jGitRepoManager, gitLabServerManager);
//
//			String tagsetId = gitProjectHandler.createTagset(
//
//					null,
//					"Test Tagset",
//					null
//			);
//
//			assertNotNull(tagsetId);
//
//			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls return
//			assertFalse(jGitRepoManager.isAttached());
//
//			jGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
//			Status status = jGitRepoManager.getGitApi().status().call();
//			Set<String> added = status.getAdded();
//
//			assert status.hasUncommittedChanges();
//			assert added.contains(".gitmodules");
//			assert added.contains(String.format("%s/%s", GitProjectHandler.TAGSET_SUBMODULES_DIRECTORY_NAME, tagsetId));
//		}
//	}
//
//	@Test
//	public void createMarkupCollection() throws Exception {
//		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(CATMAPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
//			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());
//
//			GitProjectManager gitProjectManager = new GitProjectManager(
//					CATMAPropertyKey.GitBasedRepositoryBasePath.getValue(),
//					UserIdentification.userToMap(this.catmaUser.getIdentifier()));
//
//
//			String projectId = gitProjectManager.create(
//					"Test CATMA Project",
//					"This is a test CATMA project"
//			);
//			this.projectsToDeleteOnTearDown.add(projectId);
//
//			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls return
//			assertFalse(jGitRepoManager.isAttached());
//
//			GitProjectHandler gitProjectHandler = new GitProjectHandler(null, projectId, jGitRepoManager, gitLabServerManager);
//
//			String markupCollectionId = gitProjectHandler.createMarkupCollection(
//					null,
//					"Test Markup Collection",
//					null,
//					"fakeSourceDocumentId",
//					"fakeSourceDocumentVersion"
//			);
//
//			assertNotNull(markupCollectionId);
//
//			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls return
//			assertFalse(jGitRepoManager.isAttached());
//
//			jGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
//			Status status = jGitRepoManager.getGitApi().status().call();
//			Set<String> added = status.getAdded();
//
//			assert status.hasUncommittedChanges();
//			assert added.contains(".gitmodules");
//			assert added.contains(
//					String.format(
//							"%s/%s", GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME, markupCollectionId
//					)
//			);
//		}
//	}

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

		String sourceDocumentUuid = new IDGenerator().generateDocumentId();
		String tokenizedSourceDocumentFileName = sourceDocumentUuid + "." + "json"; // GraphWorktreeProject.TOKENIZED_FILE_EXTENSION

		/*
		All of the above circumvents file upload, *ContentHandler classes and SourceDocument class
		See GraphWorktreeProject.insert
		 */

		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(
				CATMAPropertyKey.GitBasedRepositoryBasePath.getValue(), gitlabManagerRestricted.getUser()
		)) {

			directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			BackgroundService mockBackgroundService = mock(BackgroundService.class);
			EventBus mockEventBus = mock(EventBus.class);

			GitProjectManager gitProjectManager = new GitProjectManager(
					CATMAPropertyKey.GitBasedRepositoryBasePath.getValue(),
					gitlabManagerRestricted,
					(projectId) -> {}, // noop deletion handler
					mockBackgroundService,
					mockEventBus
			);

			String projectId = gitProjectManager.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			// we don't add the projectId to projectsToDeleteOnTearDown as deletion of the user will take care of that for us

			// the JGitRepoManager instance should always be in a detached state after GitProjectManager calls return
			assertFalse(jGitRepoManager.isAttached());

			GitProjectHandler gitProjectHandler = new GitProjectHandler(gitlabManagerRestricted.getUser(), projectId, jGitRepoManager, gitlabManagerRestricted);

			String sourceDocumentId = gitProjectHandler.createSourceDocument(
					sourceDocumentUuid,
					originalSourceDocumentStream, originalSourceDocument.getName(),
					convertedSourceDocumentStream, convertedSourceDocument.getName(),
					terms, tokenizedSourceDocumentFileName,
					sourceDocumentInfo
			);

			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls return
			assertFalse(jGitRepoManager.isAttached());

			jGitRepoManager.open(projectId, GitProjectManager.getProjectRootRepositoryName(projectId));
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
