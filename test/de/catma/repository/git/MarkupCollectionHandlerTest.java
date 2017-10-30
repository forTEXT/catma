package de.catma.repository.git;

import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.repository.git.exceptions.MarkupCollectionHandlerException;
import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManagerTest;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotationTest;
import de.catma.tag.*;
import helpers.Randomizer;
import mockit.Expectations;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.apache.commons.io.FileUtils;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

@RunWith(JMockit.class)
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
				"fakeSourceDocumentId", "fakeSourceDocumentVersion",
				projectId, null
			);
			// we don't add the markupCollectionId to this.markupCollectionReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			assertNotNull(markupCollectionId);
			assert markupCollectionId.startsWith("CATMA_");

			// the LocalGitRepositoryManager instance should always be in a detached state after MarkupCollectionHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			File expectedRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), markupCollectionHandler.getMarkupCollectionRepoName(markupCollectionId));

			assert expectedRepoPath.exists();
			assert expectedRepoPath.isDirectory();
			this.directoriesToDeleteOnTearDown.add(expectedRepoPath);
			assert Arrays.asList(expectedRepoPath.list()).contains("header.json");

			String expectedSerializedHeader = "" +
					"{\n" +
					"\t\"author\":null,\n" +
					"\t\"description\":null,\n" +
					"\t\"name\":\"Test Markup Collection\",\n" +
					"\t\"publisher\":null,\n" +
					"\t\"sourceDocumentId\":\"fakeSourceDocumentId\",\n" +
					"\t\"sourceDocumentVersion\":\"fakeSourceDocumentVersion\",\n" +
					"\t\"tagsets\":{}\n" +
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
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties, "fakeUserIdentifier")) {
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
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties, "fakeUserIdentifier")) {
			MarkupCollectionHandler markupCollectionHandler = new MarkupCollectionHandler(
				localGitRepoManager, this.remoteGitServerManager
			);

			ProjectHandler projectHandler = new ProjectHandler(localGitRepoManager, this.remoteGitServerManager);

			String projectId = projectHandler.create(
				"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// the LocalGitRepositoryManager instance should always be in a detached state after ProjectHandler calls
			// return
			assertFalse(localGitRepoManager.isAttached());

			String markupCollectionId = markupCollectionHandler.create(
				"Test Markup Collection", null,
				"fakeSourceDocumentId", "fakeSourceDocumentVersion",
				projectId, null
			);
			// we don't add the markupCollectionId to this.markupCollectionReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			assertNotNull(markupCollectionId);
			assert markupCollectionId.startsWith("CATMA_");

			// the LocalGitRepositoryManager instance should always be in a detached state after MarkupCollectionHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			markupCollectionHandler.addTagset(
				markupCollectionId, "fakeTagsetId", "fakeTagsetVersion"
			);

			// the LocalGitRepositoryManager instance should always be in a detached state after MarkupCollectionHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			localGitRepoManager.open(markupCollectionHandler.getMarkupCollectionRepoName(markupCollectionId));

			String expectedSerializedHeader = "" +
					"{\n" +
					"\t\"author\":null,\n" +
					"\t\"description\":null,\n" +
					"\t\"name\":\"Test Markup Collection\",\n" +
					"\t\"publisher\":null,\n" +
					"\t\"sourceDocumentId\":\"fakeSourceDocumentId\",\n" +
					"\t\"sourceDocumentVersion\":\"fakeSourceDocumentVersion\",\n" +
					"\t\"tagsets\":{\n" +
					"\t\t\"fakeTagsetId\":\"fakeTagsetVersion\"\n" +
					"\t}\n" +
					"}";

			assertEquals(
				expectedSerializedHeader.replaceAll("[\n\t]", ""),
				FileUtils.readFileToString(
					new File(localGitRepoManager.getRepositoryWorkTree(), "header.json"), StandardCharsets.UTF_8
				)
			);
		}
	}

	@Test
	public void removeTagset() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties, "fakeUserIdentifier")) {
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
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties, "fakeUserIdentifier")) {
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
				"fakeSourceDocumentId", "fakeSourceDocumentVersion",
				projectId, null
			);
			// we don't add the markupCollectionId to this.markupCollectionReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			// the LocalGitRepositoryManager instance should always be in a detached state after MarkupCollectionHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			// TODO: create a tag instance/reference for a real tag definition with real urls etc.
			String sourceDocumentUri = "http://catma.de/portal/sourcedocument/CATMA_SOURCEDOC";

			TagInstance tagInstance = JsonLdWebAnnotationTest.getFakeTagInstance();

			Range range1 = new Range(12, 18);
			Range range2 = new Range(41, 47);

			List<TagReference> tagReferences = new ArrayList<>(
				Arrays.asList(
					new TagReference(tagInstance, sourceDocumentUri, range1),
					new TagReference(tagInstance, sourceDocumentUri, range2)
				)
			);

			JsonLdWebAnnotation jsonLdWebAnnotation = new JsonLdWebAnnotation(
				this.remoteGitServerManager.getGitLabServerUrl(), projectId, tagReferences
			);

			markupCollectionHandler.addTagInstance(markupCollectionId, jsonLdWebAnnotation);

			// the LocalGitRepositoryManager instance should always be in a detached state after MarkupCollectionHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			localGitRepoManager.open(markupCollectionHandler.getMarkupCollectionRepoName(markupCollectionId));

			File expectedTagInstanceJsonFilePath = new File(
				localGitRepoManager.getRepositoryWorkTree(), "CATMA_TAG_INST.json"
			);

			assert expectedTagInstanceJsonFilePath.exists();
			assert expectedTagInstanceJsonFilePath.isFile();

			String expectedTagInstanceJsonFileContents = "" +
					"{\n" +
					"\t\"body\":{\n" +
					"\t\t\"@context\":{\n" +
					"\t\t\t\"SYSPROP_DEF\":\"http://catma.de/portal/tag/CATMA_TAG_DEF/property/CATMA_SYSPROP_DEF\",\n" +
					"\t\t\t\"UPROP_DEF\":\"http://catma.de/portal/tag/CATMA_TAG_DEF/property/CATMA_UPROP_DEF\",\n" +
					"\t\t\t\"tag\":\"http://catma.de/portal/tag\"\n" +
					"\t\t},\n" +
					"\t\t\"properties\":{\n" +
					"\t\t\t\"SYSPROP_DEF\":[\"SYSPROP_VAL_1\"],\n" +
					"\t\t\t\"UPROP_DEF\":[\"UPROP_VAL_2\"]\n" +
					"\t\t},\n" +
					"\t\t\"tag\":\"http://catma.de/portal/tag/CATMA_TAG_DEF\",\n" +
					"\t\t\"type\":\"Dataset\"\n" +
					"\t},\n" +
					"\t\"@context\":\"http://www.w3.org/ns/anno.jsonld\",\n" +
					"\t\"id\":\"http://catma.de/portal/annotation/CATMA_TAG_INST\",\n" +
					"\t\"target\":{\n" +
					"\t\t\"items\":[{\n" +
					"\t\t\t\"selector\":{\n" +
					"\t\t\t\t\"end\":18,\n" +
					"\t\t\t\t\"start\":12,\n" +
					"\t\t\t\t\"type\":\"TextPositionSelector\"\n" +
					"\t\t\t},\n" +
					"\t\t\t\"source\":\"http://catma.de/portal/sourcedocument/CATMA_SOURCEDOC\"\n" +
					"\t\t},\n" +
					"\t\t{\n" +
					"\t\t\t\"selector\":{\n" +
					"\t\t\t\t\"end\":47,\n" +
					"\t\t\t\t\"start\":41,\n" +
					"\t\t\t\t\"type\":\"TextPositionSelector\"\n" +
					"\t\t\t},\n" +
					"\t\t\t\"source\":\"http://catma.de/portal/sourcedocument/CATMA_SOURCEDOC\"\n" +
					"\t\t}],\n" +
					"\t\t\"type\":\"List\"\n" +
					"\t},\n" +
					"\t\"type\":\"Annotation\"\n" +
					"}";

			assertEquals(
				expectedTagInstanceJsonFileContents.replaceAll("[\n\t]", ""),
				FileUtils.readFileToString(expectedTagInstanceJsonFilePath, StandardCharsets.UTF_8)
			);
		}
	}

	@Test
	public void jsonLdWebAnnotationNotImplementedCheck() throws Exception {
		// this check should pass while JsonLdWebAnnotation.getTagInstance still throws a not implemented exception

		// When this test fails, check the mocking of this function in the open() test
		JsonLdWebAnnotation webAnnotation = new JsonLdWebAnnotation();
		thrown.expect(de.catma.repository.git.exceptions.JsonLdWebAnnotationException.class);
		thrown.expectMessage("Not implemented");
		webAnnotation.toTagReferenceList("fakeUserMarkupCollectionId");
	}

	@Test
	public void open() throws Exception {
		JsonLdWebAnnotation anyInstance = new JsonLdWebAnnotation();

		// TODO: Stop mocking this once getTagInstance works. The jsonLdWebAnnotationNotImplementedCheck
		// should fail at that point as a reminder.
		new Expectations(JsonLdWebAnnotation.class) {{
			anyInstance.getTagInstance(); result = JsonLdWebAnnotationTest.getFakeTagInstance();
		}};

		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties, "fakeUserIdentifier")) {
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

			String markupCollectionId = markupCollectionHandler.create(
					"Test Markup Collection", null,
					"fakeSourceDocumentId", "fakeSourceDocumentVersion",
					projectId, null
			);
			// we don't add the markupCollectionId to this.markupCollectionReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			// TODO: create a tag instance/reference for a real tag definition with real urls etc.
			String sourceDocumentUri = "http://catma.de/portal/sourcedocument/CATMA_SOURCEDOC";

			TagInstance tagInstance = JsonLdWebAnnotationTest.getFakeTagInstance();

			Range range1 = new Range(12, 18);
			Range range2 = new Range(41, 47);

			List<TagReference> tagReferences = new ArrayList<>(
					Arrays.asList(
							new TagReference(tagInstance, sourceDocumentUri, range1),
							new TagReference(tagInstance, sourceDocumentUri, range2)
					)
			);

			JsonLdWebAnnotation jsonLdWebAnnotation = new JsonLdWebAnnotation(
				this.remoteGitServerManager.getGitLabServerUrl(), projectId, tagReferences
			);

			markupCollectionHandler.addTagInstance(markupCollectionId, jsonLdWebAnnotation);

			UserMarkupCollection markupCollection = markupCollectionHandler.open(markupCollectionId);

			assertNotNull(markupCollection);

			assertEquals("Test Markup Collection", markupCollection.getContentInfoSet().getTitle());

			assertEquals(tagReferences.size(), markupCollection.getTagReferences().size());
			assertTrue(tagReferences.get(0).getRange().equals(range1));
		}

		new Verifications() {{
			anyInstance.getTagInstance();
		}};
	}
}
