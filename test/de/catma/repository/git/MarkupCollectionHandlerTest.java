package de.catma.repository.git;

import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.git.exceptions.MarkupCollectionHandlerException;
import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManagerTest;
import de.catma.repository.jsonld.TagReferenceJsonLd;
import de.catma.tag.*;
import helpers.Randomizer;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Status;
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

public class MarkupCollectionHandlerTest {
	private Properties catmaProperties;
	private RemoteGitServerManager remoteGitServerManager;

	private ArrayList<File> directoriesToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<String> markupCollectionReposToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<String> projectsToDeleteOnTearDown = new ArrayList<>();

	public MarkupCollectionHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
	}

	@Before
	public void setUp() throws Exception {
		// create a fake CATMA user which we'll use to instantiate the RemoteGitServerManager
		de.catma.user.User catmaUser = Randomizer.getDbUser();

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

		if (this.markupCollectionReposToDeleteOnTearDown.size() > 0) {
			for (String markupCollectionId : this.markupCollectionReposToDeleteOnTearDown) {
				List<Project> projects = this.remoteGitServerManager.getAdminGitLabApi().getProjectApi().getProjects(
					markupCollectionId
				); // this getProjects overload does a search
				for (Project project : projects) {
					this.remoteGitServerManager.deleteRepository(project.getId());
				}
				await().until(
					() -> this.remoteGitServerManager.getAdminGitLabApi().getProjectApi().getProjects().isEmpty()
				);
			}
			this.markupCollectionReposToDeleteOnTearDown.clear();
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
	public void create() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			MarkupCollectionHandler markupCollectionHandler = new MarkupCollectionHandler(
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

			String markupCollectionId = markupCollectionHandler.create(
				"Test Markup Collection", null,
				"fakeSourceDocumentId", projectId,
				null
			);
			// we don't add the markupCollectionId to this.markupCollectionReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			assertNotNull(markupCollectionId);
			assert markupCollectionId.startsWith("CATMA_");

			// the LocalGitRepositoryManager instance should always be in a detached state after MarkupCollectionHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			File expectedRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), markupCollectionId);

			assert expectedRepoPath.exists();
			assert expectedRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(expectedRepoPath);
			assert Arrays.asList(expectedRepoPath.list()).contains("header.json");

			String expectedSerializedHeader = "" +
					"{\n" +
					"\t\"description\":null,\n" +
					"\t\"name\":\"Test Markup Collection\",\n" +
					"\t\"sourceDocumentId\":\"fakeSourceDocumentId\"\n" +
					"}";

			assertEquals(
				expectedSerializedHeader.replaceAll("[\n\t]", ""),
				FileUtils.readFileToString(new File(expectedRepoPath, "header.json"), StandardCharsets.UTF_8)
			);
		}
	}

	// how to test for exceptions: https://stackoverflow.com/a/31826781
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void delete() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			MarkupCollectionHandler markupCollectionHandler = new MarkupCollectionHandler(
				localGitRepoManager, this.remoteGitServerManager
			);

			thrown.expect(MarkupCollectionHandlerException.class);
			thrown.expectMessage("Not implemented");
			markupCollectionHandler.delete("fake");
		}
	}

	@Test
	public void addTagset() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			MarkupCollectionHandler markupCollectionHandler = new MarkupCollectionHandler(
				localGitRepoManager, this.remoteGitServerManager
			);

			ProjectHandler projectHandler = new ProjectHandler(localGitRepoManager, this.remoteGitServerManager);

			TagsetHandler tagsetHandler = new TagsetHandler(localGitRepoManager, this.remoteGitServerManager);

			String projectId = projectHandler.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// the LocalGitRepositoryManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(localGitRepoManager.isAttached());

			String tagsetId = tagsetHandler.create("Test Tagset", null, new Version(), projectId);
			// we don't add the tagsetId to this.markupCollectionReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			// the LocalGitRepositoryManager instance should always be in a detached state after TagsetHandler calls
			// return
			assertFalse(localGitRepoManager.isAttached());

			User gitLabUser = this.remoteGitServerManager.getGitLabUser();
			String gitLabUserImpersonationToken = this.remoteGitServerManager.getGitLabUserImpersonationToken();

			// because we're adding an existing tagset, it needs to exist on the server with at least an initial commit,
			// otherwise the addition of the submodule within addTagset below won't work...
			// it won't fail, but the problem will become evident if you do a `git status`:
			// "error: cache entry has null sha1: <path-to-submodule>"
			// with status.submoduleSummary turned on in git config you would also see:
			// "Warn: <path-to-submodule> doesn't contain commit 0000000000000000000000000000000000000000"
			// under the submodule changes, or when doing a status call through JGit you would see:
			// "org.eclipse.jgit.api.errors.JGitInternalException: Missing unknown 0000000000000000000000000000000000000000"
			localGitRepoManager.open(tagsetId);
			localGitRepoManager.push(gitLabUser.getUsername(), gitLabUserImpersonationToken);

			localGitRepoManager.detach(); // can't call clone on an attached instance

			String markupCollectionId = markupCollectionHandler.create(
				"Test Markup Collection", null,
				"fakeSourceDocumentId", projectId,
				null
			);
			// we don't add the markupCollectionId to this.markupCollectionReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			assertNotNull(markupCollectionId);
			assert markupCollectionId.startsWith("CATMA_");

			// the LocalGitRepositoryManager instance should always be in a detached state after MarkupCollectionHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			markupCollectionHandler.addTagset(markupCollectionId, tagsetId);

			// the LocalGitRepositoryManager instance should always be in a detached state after MarkupCollectionHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			localGitRepoManager.open(markupCollectionId);
			Status status = localGitRepoManager.getGitApi().status().call();
			Set<String> added = status.getAdded();

			assert status.hasUncommittedChanges();
			assert added.contains(".gitmodules");
			assert added.contains("tagsets/" + tagsetId);
		}
	}

	@Test
	public void removeTagset() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			MarkupCollectionHandler markupCollectionHandler = new MarkupCollectionHandler(
				localGitRepoManager, this.remoteGitServerManager
			);

			thrown.expect(MarkupCollectionHandlerException.class);
			thrown.expectMessage("Not implemented");
			markupCollectionHandler.removeTagset("fakeMarkupCollectionId", "fakeTagsetId");
		}
	}

	@Test
	public void addTagInstance() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			MarkupCollectionHandler markupCollectionHandler = new MarkupCollectionHandler(
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

			String markupCollectionId = markupCollectionHandler.create(
				"Test Markup Collection", null,
				"fakeSourceDocumentId", projectId,
				null
			);
			// we don't add the markupCollectionId to this.markupCollectionReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			// the LocalGitRepositoryManager instance should always be in a detached state after MarkupCollectionHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			// TODO: create a tag instance/reference for a real tag definition with real urls etc.
			String uri = "http://catma.de/sourcedocument/doc1";

			Range range = new Range(42, 125);

			PropertyPossibleValueList possibleValueList = new PropertyPossibleValueList("TestPossibleValue");
			PropertyDefinition propertyDefinition = new PropertyDefinition(
				1, "CATMA_PROPDEF", "FAKE_PROP_DEF", possibleValueList
			);

			TagDefinition tagDefinition = new TagDefinition(
				1, "CATMA_1", "Weather", new Version(), null, null
			);
			tagDefinition.addUserDefinedPropertyDefinition(propertyDefinition);

			PropertyValueList instancePropertyValueList = new PropertyValueList("SimplePropertyValue");
			Property property = new Property(propertyDefinition, instancePropertyValueList);

			TagInstance tagInstance = new TagInstance("CATMA_129837", tagDefinition);
			tagInstance.addUserDefinedProperty(property);

			TagReference internalReference = new TagReference(tagInstance, uri, range);
			TagReferenceJsonLd ldWrapper = new TagReferenceJsonLd(internalReference);

			markupCollectionHandler.addTagInstance(markupCollectionId, ldWrapper);

			// the LocalGitRepositoryManager instance should always be in a detached state after MarkupCollectionHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			localGitRepoManager.open(markupCollectionId);

			File expectedTagInstanceJsonFilePath = new File(
				localGitRepoManager.getRepositoryWorkTree(), "CATMA_129837.json"
			);

			assert expectedTagInstanceJsonFilePath.exists();
			assert expectedTagInstanceJsonFilePath.isFile();

			String expectedTagInstanceJsonFileContents = "" +
					"{\n" +
					"\t\"body\":{\n" +
					"\t\t\"@context\":{\n" +
					"\t\t\t\"FAKE_PROP_DEF\":\"http://catma.de/portal/tag/CATMA_1/property/CATMA_PROPDEF\",\n" +
					"\t\t\t\"tag\":\"http://catma.de/portal/tag\"\n" +
					"\t\t},\n" +
					"\t\t\"properties\":{\n" +
					"\t\t\t\"FAKE_PROP_DEF\":\"SimplePropertyValue\"\n" +
					"\t\t},\n" +
					"\t\t\"tag\":\"http://catma.de/portal/tag/CATMA_1\",\n" +
					"\t\t\"type\":\"Dataset\"\n" +
					"\t},\n" +
					"\t\"@context\":\"http://www.w3.org/ns/anno.jsonld\",\n" +
					"\t\"id\":\"http://catma.de/portal/annotation/CATMA_129837\",\n" +
					"\t\"target\":{\n" +
					"\t\t\"source\":\"http://catma.de/sourcedocument/doc1\",\n" +
					"\t\t\"TextPositionSelector\":{\n" +
					"\t\t\t\"end\":125,\n" +
					"\t\t\t\"start\":42\n" +
					"\t\t}\n" +
					"\t},\n" +
					"\t\"type\":\"Annotation\"\n" +
					"}";

			assertEquals(
				expectedTagInstanceJsonFileContents.replaceAll("[\n\t]", ""),
				FileUtils.readFileToString(expectedTagInstanceJsonFilePath, StandardCharsets.UTF_8)
			);
		}
	}
}
