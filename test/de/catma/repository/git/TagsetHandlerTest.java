package de.catma.repository.git;

import de.catma.repository.git.exceptions.TagsetHandlerException;
import de.catma.repository.git.managers.GitLabServerManagerTest;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.repository.git.managers.GitLabServerManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.model_wrappers.GitTagDefinition;
import de.catma.repository.git.serialization.models.TagsetDefinitionHeader;
import de.catma.repository.git.serialization.models.json_ld.JsonLdWebAnnotationTest;
import de.catma.tag.*;
import de.catma.util.IDGenerator;
import org.apache.commons.io.FileUtils;
import helpers.Randomizer;
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
import java.nio.file.Paths;
import java.util.*;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

public class TagsetHandlerTest {
	private Properties catmaProperties;
	private de.catma.user.User catmaUser;
	private GitLabServerManager gitLabServerManager;

	private ArrayList<File> directoriesToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<String> tagsetReposToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<String> projectsToDeleteOnTearDown = new ArrayList<>();

	private IDGenerator idGenerator = null;

	public TagsetHandlerTest() throws Exception {
		String propertiesFile = System.getProperties().containsKey("prop") ?
				System.getProperties().getProperty("prop") : "catma.properties";

		this.catmaProperties = new Properties();
		this.catmaProperties.load(new FileInputStream(propertiesFile));

		this.idGenerator = new IDGenerator();
	}

	@Before
	public void setUp() throws Exception {
		// create a fake CATMA user which we'll use to instantiate the GitLabServerManager & JGitRepoManager
		this.catmaUser = Randomizer.getDbUser();

		this.gitLabServerManager = new GitLabServerManager(this.catmaProperties, catmaUser);
		this.gitLabServerManager.replaceGitLabServerUrl = true;
	}

	@After
	public void tearDown() throws Exception {
		if (this.directoriesToDeleteOnTearDown.size() > 0) {
			for (File dir : this.directoriesToDeleteOnTearDown) {
				FileUtils.deleteDirectory(dir);
			}
			this.directoriesToDeleteOnTearDown.clear();
		}

		if (this.tagsetReposToDeleteOnTearDown.size() > 0) {
			for (String tagsetId : this.tagsetReposToDeleteOnTearDown) {
				String tagsetRepoName = TagsetHandler.getTagsetRepositoryName(tagsetId);
				List<Project> projects = this.gitLabServerManager.getAdminGitLabApi().getProjectApi().getProjects(
					tagsetRepoName
				); // this getProjects overload does a search
				for (Project project : projects) {
					this.gitLabServerManager.deleteRepository(project.getId());
				}
				await().until(
					() -> this.gitLabServerManager.getAdminGitLabApi().getProjectApi().getProjects().isEmpty()
				);
			}
			this.tagsetReposToDeleteOnTearDown.clear();
		}

		if (this.projectsToDeleteOnTearDown.size() > 0) {
			try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
				ProjectHandler projectHandler = new ProjectHandler(jGitRepoManager, this.gitLabServerManager);

				for (String projectId : this.projectsToDeleteOnTearDown) {
					projectHandler.delete(projectId);
				}
				this.projectsToDeleteOnTearDown.clear();
			}
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
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			ProjectHandler projectHandler = new ProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = projectHandler.create(
				"Test CATMA Project for Tagset", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// the JGitRepoManager instance should always be in a detached state after ProjectHandler
			// calls return
			assertFalse(jGitRepoManager.isAttached());

			TagsetHandler tagsetHandler = new TagsetHandler(jGitRepoManager, this.gitLabServerManager);

			String name = "Test Tagset";
			String description = "Test Tagset Description";

			String tagsetId = tagsetHandler.create(projectId, null, name, description);
			// we don't add the tagsetId to this.tagsetReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			assertNotNull(tagsetId);
			assert tagsetId.startsWith("CATMA_");

			File expectedRepoPath = new File(
				jGitRepoManager.getRepositoryBasePath(), TagsetHandler.getTagsetRepositoryName(tagsetId)
			);
			assert expectedRepoPath.exists();
			assert expectedRepoPath.isDirectory();

			assert Arrays.asList(expectedRepoPath.list()).contains("header.json");

			TagsetDefinitionHeader expectedHeader = new TagsetDefinitionHeader(name, description);

			String serialized = FileUtils.readFileToString(
				new File(expectedRepoPath, "header.json"), StandardCharsets.UTF_8
			);
			TagsetDefinitionHeader actualHeader = new SerializationHelper<TagsetDefinitionHeader>().deserialize(
				serialized, TagsetDefinitionHeader.class
			);

			assertEquals(expectedHeader.getName(), actualHeader.getName());
			assertEquals(expectedHeader.getDescription(), actualHeader.getDescription());
		}
	}

	// how to test for exceptions: https://stackoverflow.com/a/31826781
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void delete() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
			TagsetHandler tagsetHandler = new TagsetHandler(jGitRepoManager, this.gitLabServerManager);

			thrown.expect(TagsetHandlerException.class);
			thrown.expectMessage("Not implemented");
			tagsetHandler.delete("fakeProjectId", "fakeTagsetId");
		}
	}

	@Test
	public void createTagDefinitionWithoutParent() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			// create a project
			ProjectHandler projectHandler = new ProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = projectHandler.create(
					"Test CATMA Project for Tagset", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// create a tagset
			String tagsetId = projectHandler.createTagset(
					projectId, null, "Test Tagset", null
			);
			// we don't add the tagsetId to this.tagsetReposToDeleteOnTearDown as deletion of the project will take
			// care of that for us

			// create a TagDefinition object
			String tagDefinitionId = this.idGenerator.generate();
			Version tagDefinitionVersion = new Version();

			TagDefinition tagDefinition = new TagDefinition(
					null, tagDefinitionId, "FakeTagDefinitionName", tagDefinitionVersion,
					null, null
			);

			PropertyDefinition propertyDefinition = new PropertyDefinition(
					null, "CATMA_fakeUserPropDefUuid", "Weather",
					new PropertyPossibleValueList(
							Arrays.asList("Good", "Bad", "Toto, I've a feeling we're not in Kansas anymore."),
							true
					)
			);
			tagDefinition.addUserDefinedPropertyDefinition(propertyDefinition);

			// call createTagDefinition
			TagsetHandler tagsetHandler = new TagsetHandler(jGitRepoManager, this.gitLabServerManager);
			String returnedTagDefinitionId = tagsetHandler.createTagDefinition(projectId, tagsetId, tagDefinition);

			assertNotNull(returnedTagDefinitionId);
			assert returnedTagDefinitionId.startsWith("CATMA_");

			// the JGitRepoManager instance should always be in a detached state after TagsetHandler calls return
			assertFalse(jGitRepoManager.isAttached());

			assertEquals(tagDefinitionId, returnedTagDefinitionId);

			String projectRootRepositoryName = ProjectHandler.getProjectRootRepositoryName(projectId);

			File expectedTagDefinitionPath = Paths.get(
					jGitRepoManager.getRepositoryBasePath().toString(),
					projectRootRepositoryName,
					ProjectHandler.TAGSET_SUBMODULES_DIRECTORY_NAME,
					tagsetId,
					tagDefinition.getUuid()
			).toFile();

			assert expectedTagDefinitionPath.exists() : "Directory does not exist";
			assert expectedTagDefinitionPath.isDirectory() : "Path is not a directory";

			assert Arrays.asList(expectedTagDefinitionPath.list()).contains("propertydefs.json");

			GitTagDefinition expectedGitTagDefinition = new GitTagDefinition(tagDefinition);

			String actualSerializedGitTagDefinition = FileUtils.readFileToString(
				new File(expectedTagDefinitionPath, "propertydefs.json"), StandardCharsets.UTF_8
			);
			GitTagDefinition actualGitTagDefinition = new SerializationHelper<GitTagDefinition>().deserialize(
				actualSerializedGitTagDefinition, GitTagDefinition.class
			);

			assertEquals(
					expectedGitTagDefinition.getTagsetDefinitionUuid(),
					actualGitTagDefinition.getTagsetDefinitionUuid()
			);
			assertEquals(expectedGitTagDefinition.getParentUuid(), actualGitTagDefinition.getParentUuid());
			assertEquals(expectedGitTagDefinition.getUuid(), actualGitTagDefinition.getUuid());
			assertEquals(expectedGitTagDefinition.getName(), actualGitTagDefinition.getName());

			// TODO: assert tag definition and properties
		}
	}

	@Test
	public void createTagDefinitionWithParent() throws Exception {
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			// create a project
			ProjectHandler projectHandler = new ProjectHandler(jGitRepoManager, this.gitLabServerManager);

			String projectId = projectHandler.create(
					"Test CATMA Project for Tagset", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// create a tagset
			String tagsetId = projectHandler.createTagset(
					projectId, null, "Test Tagset", null
			);
			// we don't add the tagsetId to this.tagsetReposToDeleteOnTearDown as deletion of the project will take
			// care of that for us

			// create a TagDefinition object that has a (fake) parent
			String tagDefinitionId = this.idGenerator.generate();
			String parentTagDefinitionId = this.idGenerator.generate();
			Version tagDefinitionVersion = new Version();

			TagDefinition tagDefinition = new TagDefinition(
					null, tagDefinitionId, "FakeTagDefinitionName", tagDefinitionVersion,
					null, parentTagDefinitionId
			);

			// call createTagDefinition
			TagsetHandler tagsetHandler = new TagsetHandler(jGitRepoManager, this.gitLabServerManager);
			String returnedTagDefinitionId = tagsetHandler.createTagDefinition(projectId, tagsetId, tagDefinition);

			assertNotNull(returnedTagDefinitionId);
			assert returnedTagDefinitionId.startsWith("CATMA_");

			// the JGitRepoManager instance should always be in a detached state after TagsetHandler calls return
			assertFalse(jGitRepoManager.isAttached());

			assertEquals(tagDefinitionId, returnedTagDefinitionId);

			String projectRootRepositoryName = ProjectHandler.getProjectRootRepositoryName(projectId);

			File expectedTagDefinitionPath = Paths.get(
					jGitRepoManager.getRepositoryBasePath().toString(),
					projectRootRepositoryName,
					ProjectHandler.TAGSET_SUBMODULES_DIRECTORY_NAME,
					tagsetId,
					parentTagDefinitionId,
					tagDefinition.getUuid()
			).toFile();

			assert expectedTagDefinitionPath.exists() : "Directory does not exist";
			assert expectedTagDefinitionPath.isDirectory() : "Path is not a directory";

			assert Arrays.asList(expectedTagDefinitionPath.list()).contains("propertydefs.json");

			GitTagDefinition expectedGitTagDefinition = new GitTagDefinition(tagDefinition);

			String actualSerializedGitTagDefinition = FileUtils.readFileToString(
					new File(expectedTagDefinitionPath, "propertydefs.json"), StandardCharsets.UTF_8
			);
			GitTagDefinition actualGitTagDefinition = new SerializationHelper<GitTagDefinition>().deserialize(
					actualSerializedGitTagDefinition, GitTagDefinition.class
			);

			assertEquals(
					expectedGitTagDefinition.getTagsetDefinitionUuid(),
					actualGitTagDefinition.getTagsetDefinitionUuid()
			);
			assertEquals(expectedGitTagDefinition.getParentUuid(), actualGitTagDefinition.getParentUuid());
			assertEquals(expectedGitTagDefinition.getUuid(), actualGitTagDefinition.getUuid());
			assertEquals(expectedGitTagDefinition.getName(), actualGitTagDefinition.getName());

			// TODO: assert tag definition
		}
	}

	@Test
	public void open() throws Exception {
		// TODO: don't hardcode anything in assertions (tagset name, tag definition UUID...)
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			HashMap<String, Object> getJsonLdWebAnnotationResult = JsonLdWebAnnotationTest.getJsonLdWebAnnotation(
					jGitRepoManager, this.gitLabServerManager
			);

			String projectId = (String)getJsonLdWebAnnotationResult.get("projectUuid");
			String tagsetId = (String)getJsonLdWebAnnotationResult.get("tagsetDefinitionUuid");
			String tagDefinitionId = (String)getJsonLdWebAnnotationResult.get("tagDefinitionUuid");

			this.projectsToDeleteOnTearDown.add(projectId);

			TagsetHandler tagsetHandler = new TagsetHandler(jGitRepoManager, this.gitLabServerManager);

			TagsetDefinition loadedTagsetDefinition = tagsetHandler.open(projectId, tagsetId);

			assertEquals(tagsetId, loadedTagsetDefinition.getUuid());
			assertEquals("Test Tagset", loadedTagsetDefinition.getName());
			assertNotNull(loadedTagsetDefinition.getRevisionHash());

			assertFalse(loadedTagsetDefinition.isEmpty());

			TagDefinition loadedTagDefinition = loadedTagsetDefinition.getTagDefinition(tagDefinitionId);

			assertNotNull(loadedTagDefinition);

			assertEquals(tagDefinitionId, loadedTagDefinition.getUuid());
			assertEquals("TAG_DEF", loadedTagDefinition.getName());
			assertEquals("", loadedTagDefinition.getParentUuid());
		}
	}
}
