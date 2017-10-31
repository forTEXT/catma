package de.catma.repository.git;

import de.catma.document.source.*;
import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManagerTest;
import de.catma.repository.git.serialization.model_wrappers.GitSourceDocumentInfo;
import de.catma.tag.Version;
import helpers.Randomizer;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.submodule.SubmoduleStatus;
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
	private RemoteGitServerManager remoteGitServerManager;

	private ArrayList<String> projectsToDeleteOnTearDown = new ArrayList<>();

	public ProjectHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
	}

	@Before
	public void setUp() throws Exception {
		// create a fake CATMA user which we'll use to instantiate the RemoteGitServerManager
		de.catma.user.User catmaUser = Randomizer.getDbUser();

		this.remoteGitServerManager = new RemoteGitServerManager(
			this.catmaProperties, catmaUser
		);
		this.remoteGitServerManager.replaceGitLabServerUrl = true;
	}

	@After
	public void tearDown() throws Exception {
		if (this.projectsToDeleteOnTearDown.size() > 0) {
			try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties, "fakeUserIdentifier")) {
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
	public void create() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties, "fakeUserIdentifier")) {
			ProjectHandler projectHandler = new ProjectHandler(localGitRepoManager, this.remoteGitServerManager);

			String projectId = projectHandler.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			assertNotNull(projectId);
			assert projectId.startsWith("CATMA_");

			// the LocalGitRepositoryManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(localGitRepoManager.isAttached());

			String expectedRootRepositoryName = projectHandler.getProjectRootRepositoryName(projectId);
			String repositoryBasePath = String.format("%s/%s", this.catmaProperties.getProperty("GitBasedRepositoryBasePath"), "fakeUserIdentifier");

			File expectedRootRepositoryPath = new File(repositoryBasePath, expectedRootRepositoryName);

			assert expectedRootRepositoryPath.exists();
			assert expectedRootRepositoryPath.isDirectory();

			assert Arrays.asList(expectedRootRepositoryPath.list()).contains("tagsets.json");
			assertEquals(
				"", FileUtils.readFileToString(
						new File(expectedRootRepositoryPath, "tagsets.json"), StandardCharsets.UTF_8)
			);
		}
	}

	@Test
	public void delete() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties, "fakeUserIdentifier")) {
			ProjectHandler projectHandler = new ProjectHandler(localGitRepoManager, this.remoteGitServerManager);

			String projectId = projectHandler.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			// we don't add the projectId to this.projectsToDeleteOnTearDown as this is the delete test

			assertNotNull(projectId);
			assert projectId.startsWith("CATMA_");

			// the LocalGitRepositoryManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(localGitRepoManager.isAttached());

			String expectedRootRepositoryName = projectHandler.getProjectRootRepositoryName(projectId);
			String repositoryBasePath = String.format("%s/%s", this.catmaProperties.getProperty("GitBasedRepositoryBasePath"), "fakeUserIdentifier");

			File expectedRootRepositoryPath = new File(repositoryBasePath, expectedRootRepositoryName);

			assert expectedRootRepositoryPath.exists();
			assert expectedRootRepositoryPath.isDirectory();

			projectHandler.delete(projectId);

			assertFalse(expectedRootRepositoryPath.exists());

			// the LocalGitRepositoryManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(localGitRepoManager.isAttached());
		}
	}

	@Test
	public void insertSourceDocument() throws Exception {
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

		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties, "fakeUserIdentifier")) {
			ProjectHandler projectHandler = new ProjectHandler(localGitRepoManager, this.remoteGitServerManager);

			String projectId = projectHandler.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// the LocalGitRepositoryManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(localGitRepoManager.isAttached());

			String sourceDocumentId = projectHandler.insertSourceDocument(
				projectId,
				originalSourceDocumentStream, originalSourceDocument.getName(),
				convertedSourceDocumentStream, convertedSourceDocument.getName(),
				gitSourceDocumentInfo, null
			);

			// the LocalGitRepositoryManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(localGitRepoManager.isAttached());

			localGitRepoManager.open(projectHandler.getProjectRootRepositoryName(projectId));
			Status status = localGitRepoManager.getGitApi().status().call();
			Set<String> added = status.getAdded();

			assert status.hasUncommittedChanges();
			assert added.contains(".gitmodules");
			assert added.contains("documents/" + sourceDocumentId);
		}
	}

	@Test
	public void addTagsetToMarkupCollection() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties, "fakeUserIdentifier")) {
			ProjectHandler projectHandler = new ProjectHandler(localGitRepoManager, this.remoteGitServerManager);
			TagsetHandler tagsetHandler = new TagsetHandler(localGitRepoManager, this.remoteGitServerManager);
			MarkupCollectionHandler markupCollectionHandler = new MarkupCollectionHandler(
				localGitRepoManager, this.remoteGitServerManager
			);

			// create a project
			String projectId = projectHandler.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			assertNotNull(projectId);
			assert projectId.startsWith("CATMA_");

			// the LocalGitRepositoryManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(localGitRepoManager.isAttached());

			// create a tagset
			String tagsetId = tagsetHandler.create("Test Tagset", null, new Version(), projectId);

			// the LocalGitRepositoryManager instance should always be in a detached state after TagsetHandler calls
			// return
			assertFalse(localGitRepoManager.isAttached());

			// re-open the tagset repo to get the commit hash
			localGitRepoManager.open(TagsetHandler.getTagsetRepositoryName(tagsetId));
			ObjectId tagsetHead = localGitRepoManager.getGitApi().getRepository().resolve(Constants.HEAD);
			String tagsetCommitHash = tagsetHead.getName();

			localGitRepoManager.detach();  // can't call clone on an attached instance

			// create a markup collection
			String markupCollectionId = markupCollectionHandler.create(
				"Test Markup Collection", null,
				"fakeSourceDocumentId", "fakeSourceDocumentVersion",
				projectId, null
			);

			// the LocalGitRepositoryManager instance should always be in a detached state after MarkupCollectionHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			User gitLabUser = this.remoteGitServerManager.getGitLabUser();
			String gitLabUserImpersonationToken = this.remoteGitServerManager.getGitLabUserImpersonationToken();

			// re-open the markup collection repo to get the commit hash and because we need to push
			localGitRepoManager.open(markupCollectionHandler.getMarkupCollectionRepoName(markupCollectionId));
			ObjectId markupCollectionHead = localGitRepoManager.getGitApi().getRepository().resolve(Constants.HEAD);
			String markupCollectionCommitHash = markupCollectionHead.getName();

			// push the markup collection repo to the server so that it can be added as a submodule to the project
			// TODO: the markup collection should already be a submodule in the project at the time
			// addTagsetToMarkupCollection is called
			localGitRepoManager.push(gitLabUser.getUsername(), gitLabUserImpersonationToken);

			localGitRepoManager.detach();  // can't call open on an attached instance

			projectHandler.addTagsetToMarkupCollection(projectId, markupCollectionId, tagsetId, tagsetCommitHash);

			// the LocalGitRepositoryManager instance should always be in a detached state after ProjectHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			// assert that the markup collection submodule in the project is pointing at the correct commit hash
			String projectRepoName = projectHandler.getProjectRootRepositoryName(projectId);
			localGitRepoManager.open(projectRepoName);
			Map<String, SubmoduleStatus> statusMap = localGitRepoManager.getGitApi().submoduleStatus().call();

			assertEquals(1, statusMap.size());

			SubmoduleStatus status = statusMap.get(String.format("collections/%s", markupCollectionId));
			assertEquals(markupCollectionCommitHash, status.getHeadId().getName());
		}
	}
}
