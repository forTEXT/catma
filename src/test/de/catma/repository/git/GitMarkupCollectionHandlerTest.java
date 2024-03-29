package de.catma.repository.git;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.ProjectFilter;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.catma.document.Range;
import de.catma.document.repository.RepositoryProperties;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.managers.GitLabServerManager;
import de.catma.repository.git.managers.GitLabServerManagerTest;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotation;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotationTest;
import helpers.Randomizer;
import helpers.UserIdentification;

public class GitMarkupCollectionHandlerTest {
	private Properties catmaProperties;
	private de.catma.user.User catmaUser;
	private GitLabServerManager gitLabServerManager;

	private ArrayList<File> directoriesToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<String> markupCollectionReposToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<String> projectsToDeleteOnTearDown = new ArrayList<>();

	public GitMarkupCollectionHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));
	}


	@Before
	public void setUp() throws Exception {
		// create a fake CATMA user which we'll use to instantiate the GitLabServerManager & JGitRepoManager
		this.catmaUser = Randomizer.getDbUser();
		RepositoryProperties.INSTANCE.setProperties(catmaProperties);
		this.gitLabServerManager = new GitLabServerManager(
				UserIdentification.userToMap(this.catmaUser.getIdentifier()));
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
				List<Project> projects = this.gitLabServerManager.getAdminGitLabApi().getProjectApi().getProjects(
						new ProjectFilter().withSearch(markupCollectionId).withSimple(true)
				);
				for (Project project : projects) {
					this.gitLabServerManager.deleteRepository(project.getId());
				}
				await().until(
					() -> this.gitLabServerManager.getAdminGitLabApi().getProjectApi().getProjects(new ProjectFilter().withSimple(true)).isEmpty()
				);
			}
			this.markupCollectionReposToDeleteOnTearDown.clear();
		}

		if (this.projectsToDeleteOnTearDown.size() > 0) {
			GitProjectManager gitProjectHandler = new GitProjectManager(
					RepositoryPropertyKey.GitBasedRepositoryBasePath.getValue(),
					UserIdentification.userToMap(this.catmaUser.getIdentifier()));

			for (String projectId : this.projectsToDeleteOnTearDown) {
				gitProjectHandler.delete(projectId);
			}
			this.projectsToDeleteOnTearDown.clear();
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
		try (ILocalGitRepositoryManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			GitProjectManager gitProjectHandler = new GitProjectManager(
					RepositoryPropertyKey.GitBasedRepositoryBasePath.getValue(),
					UserIdentification.userToMap(this.catmaUser.getIdentifier()));

			String projectId = gitProjectHandler.create(
					"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// the JGitRepoManager instance should always be in a detached state after GitProjectHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());

			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
					jGitRepoManager, this.gitLabServerManager
			);

			String markupCollectionId = gitMarkupCollectionHandler.create(
					projectId,
					null,
					"Test Markup Collection",
					null,
					"fakeSourceDocumentId",
					"fakeSourceDocumentVersion"
			);
			// we don't add the markupCollectionId to this.markupCollectionReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			assertNotNull(markupCollectionId);
			assert markupCollectionId.startsWith("CATMA_");

			// the JGitRepoManager instance should always be in a detached state after GitMarkupCollectionHandler
			// calls return
			assertFalse(jGitRepoManager.isAttached());

			File expectedRepoPath = new File(
				jGitRepoManager.getRepositoryBasePath(),
				GitMarkupCollectionHandler.getMarkupCollectionRepositoryName(markupCollectionId)
			);

			assert expectedRepoPath.exists();
			assert expectedRepoPath.isDirectory();
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
		try (ILocalGitRepositoryManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
				jGitRepoManager, this.gitLabServerManager
			);

			thrown.expect(IOException.class);
			thrown.expectMessage("Not implemented");
			gitMarkupCollectionHandler.delete("fakeProjectId", "fakeMarkupCollectionId");
		}
	}

	@Test
	public void addTagset() throws Exception {
		try (ILocalGitRepositoryManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			// create a project
			GitProjectManager gitProjectManager = new GitProjectManager(
					RepositoryPropertyKey.GitBasedRepositoryBasePath.getValue(),
					UserIdentification.userToMap(this.catmaUser.getIdentifier()));

			String projectId = gitProjectManager.create(
					"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			GitProjectHandler gitProjectHandler = new GitProjectHandler(null, projectId, jGitRepoManager, gitLabServerManager);

			// create a tagset
			String tagsetId = gitProjectHandler.createTagset(
					null, "Test Tagset", null
			);
			// we don't add the tagsetId to this.tagsetReposToDeleteOnTearDown as deletion of the project will take
			// care of that for us

			// create a markup collection
			String markupCollectionId = gitProjectHandler.createMarkupCollection(
					null,"Test Markup Collection", null,
					"fakeSourceDocumentId", "fakeSourceDocumentVersion"
			);
			// we don't add the markupCollectionId to this.markupCollectionReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			// add the tagset to the markup collection
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
					jGitRepoManager, this.gitLabServerManager
			);

			gitMarkupCollectionHandler.addTagset(
					projectId, markupCollectionId, tagsetId, "fakeTagsetVersion"
			);

			// the JGitRepoManager instance should always be in a detached state after GitMarkupCollectionHandler
			// calls return
			assertFalse(jGitRepoManager.isAttached());

			// assert that the markup collection header file was updated as expected
			String expectedSerializedHeader = "" +
					"{\n" +
					"\t\"author\":null,\n" +
					"\t\"description\":null,\n" +
					"\t\"name\":\"Test Markup Collection\",\n" +
					"\t\"publisher\":null,\n" +
					"\t\"sourceDocumentId\":\"fakeSourceDocumentId\",\n" +
					"\t\"sourceDocumentVersion\":\"fakeSourceDocumentVersion\",\n" +
					"\t\"tagsets\":{\n" +
					"\t\t\"%s\":\"fakeTagsetVersion\"\n" +
					"\t}\n" +
					"}";

			expectedSerializedHeader = String.format(expectedSerializedHeader, tagsetId);

			File markupCollectionHeaderFilePath = Paths.get(
					jGitRepoManager.getRepositoryBasePath().toString(),
					GitProjectManager.getProjectRootRepositoryName(projectId),
					GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME,
					markupCollectionId,
					"header.json"
			).toFile();

			assertEquals(
				expectedSerializedHeader.replaceAll("[\n\t]", ""),
				FileUtils.readFileToString(markupCollectionHeaderFilePath, StandardCharsets.UTF_8)
			);
		}
	}

	@Test
	public void removeTagset() throws Exception {
		try (ILocalGitRepositoryManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
				jGitRepoManager, this.gitLabServerManager
			);

			thrown.expect(IOException.class);
			thrown.expectMessage("Not implemented");
			gitMarkupCollectionHandler.removeTagset(
					"fakeProjectId","fakeMarkupCollectionId", "fakeTagsetId"
			);
		}
	}

	@Test
	public void createTagInstance() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			HashMap<String, Object> getJsonLdWebAnnotationResult = JsonLdWebAnnotationTest.getJsonLdWebAnnotation(
					jGitRepoManager, this.gitLabServerManager, this.catmaUser
			);
			JsonLdWebAnnotation jsonLdWebAnnotation = (JsonLdWebAnnotation) getJsonLdWebAnnotationResult.get(
					"jsonLdWebAnnotation"
			);

			String projectId = (String)getJsonLdWebAnnotationResult.get("projectUuid");
			String markupCollectionId = (String)getJsonLdWebAnnotationResult.get("userMarkupCollectionUuid");
			String tagsetId = (String)getJsonLdWebAnnotationResult.get("tagsetDefinitionUuid");
			String tagInstanceId = (String)getJsonLdWebAnnotationResult.get("tagInstanceUuid");

			this.projectsToDeleteOnTearDown.add(projectId);

			// add the tagset to the markup collection
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
					jGitRepoManager, this.gitLabServerManager
			);

			// TODO: use the real tagset hash - currently the handler does not validate it at all
			gitMarkupCollectionHandler.addTagset(
					projectId, markupCollectionId, tagsetId, "fakeTagsetVersion"
			);

			// the JGitRepoManager instance should always be in a detached state after GitMarkupCollectionHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());

			// create the tag instance
			gitMarkupCollectionHandler.createTagInstance(
					projectId,
					markupCollectionId,
					jsonLdWebAnnotation
			);

			// the JGitRepoManager instance should always be in a detached state after GitMarkupCollectionHandler
			// calls return
			assertFalse(jGitRepoManager.isAttached());

			String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);
			jGitRepoManager.open(projectId, projectRootRepositoryName);

			File expectedTagInstanceJsonFilePath = new File(
					jGitRepoManager.getRepositoryWorkTree(),
					String.format(
							"%s/%s/annotations/%s.json",
							GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME,
							markupCollectionId,
							tagInstanceId
					)
			);

			assert expectedTagInstanceJsonFilePath.exists();
			assert expectedTagInstanceJsonFilePath.isFile();

			String expectedTagInstanceJsonFileContents = JsonLdWebAnnotationTest.EXPECTED_SERIALIZED_ANNOTATION
					.replaceAll("[\n\t]", "");
			expectedTagInstanceJsonFileContents = String.format(
					expectedTagInstanceJsonFileContents,
					getJsonLdWebAnnotationResult.get("projectRootRepositoryName"),
					getJsonLdWebAnnotationResult.get("tagsetDefinitionUuid"),
					getJsonLdWebAnnotationResult.get("tagDefinitionUuid"),
					getJsonLdWebAnnotationResult.get("userPropertyDefinitionUuid"),
					getJsonLdWebAnnotationResult.get("systemPropertyDefinitionUuid"),
					getJsonLdWebAnnotationResult.get("userMarkupCollectionUuid"),
					getJsonLdWebAnnotationResult.get("tagInstanceUuid"),
					getJsonLdWebAnnotationResult.get("sourceDocumentUuid")
			);

			assertEquals(
				expectedTagInstanceJsonFileContents,
				FileUtils.readFileToString(expectedTagInstanceJsonFilePath, StandardCharsets.UTF_8)
			);
		}
	}

	@Test
	public void open() throws Exception {
		// TODO: don't hardcode anything in assertions (markup collection name...)
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties.getProperty(RepositoryPropertyKey.GitBasedRepositoryBasePath.name()), this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			HashMap<String, Object> getJsonLdWebAnnotationResult = JsonLdWebAnnotationTest.getJsonLdWebAnnotation(
					jGitRepoManager, this.gitLabServerManager, this.catmaUser
			);
			JsonLdWebAnnotation jsonLdWebAnnotation = (JsonLdWebAnnotation)getJsonLdWebAnnotationResult.get(
					"jsonLdWebAnnotation"
			);

			String projectId = (String)getJsonLdWebAnnotationResult.get("projectUuid");
			String markupCollectionId = (String)getJsonLdWebAnnotationResult.get("userMarkupCollectionUuid");
			String tagsetId = (String)getJsonLdWebAnnotationResult.get("tagsetDefinitionUuid");

			this.projectsToDeleteOnTearDown.add(projectId);

			// add the tagset to the markup collection
			GitMarkupCollectionHandler gitMarkupCollectionHandler = new GitMarkupCollectionHandler(
					jGitRepoManager, this.gitLabServerManager
			);

			// TODO: use the real tagset hash - currently the handler does not validate it at all
			gitMarkupCollectionHandler.addTagset(
					projectId, markupCollectionId, tagsetId, "fakeTagsetVersion"
			);

			// the JGitRepoManager instance should always be in a detached state after GitMarkupCollectionHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());

			// create the tag instance within the markup collection
			gitMarkupCollectionHandler.createTagInstance(projectId, markupCollectionId, jsonLdWebAnnotation);

			// the JGitRepoManager instance should always be in a detached state after GitMarkupCollectionHandler calls
			// return
			assertFalse(jGitRepoManager.isAttached());

			UserMarkupCollection markupCollection = gitMarkupCollectionHandler.open(
					projectId, markupCollectionId
			);

			// the JGitRepoManager instance should always be in a detached state after GitMarkupCollectionHandler
			// calls return
			assertFalse(jGitRepoManager.isAttached());

			assertNotNull(markupCollection);
			assertEquals("Test Markup Collection", markupCollection.getContentInfoSet().getTitle());
			assertEquals(2, markupCollection.getTagReferences().size());
			assertTrue(markupCollection.getTagReferences().get(0).getRange().equals(new Range(12, 18)));
			assertNotNull(markupCollection.getRevisionHash());
		}
	}
}
