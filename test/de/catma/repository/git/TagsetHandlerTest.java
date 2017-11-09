package de.catma.repository.git;

import de.catma.repository.git.exceptions.TagsetHandlerException;
import de.catma.repository.git.managers.GitLabServerManagerTest;
import de.catma.repository.git.managers.JGitRepoManager;
import de.catma.repository.git.managers.GitLabServerManager;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.model_wrappers.GitTagDefinition;
import de.catma.repository.git.serialization.models.TagsetDefinitionHeader;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
			tagsetHandler.delete("fake");
		}
	}

	@Test
	public void addTagDefinitionWithoutParent() throws Exception {
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

			// call addTagDefinition
			TagsetHandler tagsetHandler = new TagsetHandler(jGitRepoManager, this.gitLabServerManager);
			String returnedTagDefinitionId = tagsetHandler.addTagDefinition(projectId, tagsetId, tagDefinition);

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
	public void addTagDefinitionWithParent() throws Exception {
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

			// call addTagDefinition
			TagsetHandler tagsetHandler = new TagsetHandler(jGitRepoManager, this.gitLabServerManager);
			String returnedTagDefinitionId = tagsetHandler.addTagDefinition(projectId, tagsetId, tagDefinition);

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
		try (JGitRepoManager jGitRepoManager = new JGitRepoManager(this.catmaProperties, this.catmaUser)) {
			this.directoriesToDeleteOnTearDown.add(jGitRepoManager.getRepositoryBasePath());

			// TODO: use JsonLdWebAnnotationTest.getTagInstance once it's been implemented
			// for now, we need to create a fake project repo with fake submodules to make this test pass
			File fakeProjectPath = new File(jGitRepoManager.getRepositoryBasePath(), "fakeProjectId_corpus");
			// need to init the fake project repo, otherwise JGitRepoManager will fail to open it later
			jGitRepoManager.init(fakeProjectPath.getName(), null);
			jGitRepoManager.detach();  // can't call open on an attached instance

			File fakeTagsetSubmodulePath = new File(fakeProjectPath, "tagsets/CATMA_TAGSET_DEF_tagset");

			File fakeTagsetHeaderFilePath = new File(fakeTagsetSubmodulePath, "header.json");
			String fakeSerializedTagsetHeader = "" +
					"{\n" +
					"\t\"description\":\"\",\n" +
					"\t\"name\":\"TAGSET_DEF\"\n" +
					"}";
			FileUtils.writeStringToFile(fakeTagsetHeaderFilePath, fakeSerializedTagsetHeader, StandardCharsets.UTF_8);

			File fakeTagDefinitionPath = new File(fakeTagsetSubmodulePath, "CATMA_TAG_DEF");

			File fakeTagDefinitionPropertyDefsFilePath = new File(fakeTagDefinitionPath, "propertydefs.json");
			String fakeSerializedTagDefinition = "" +
					"{\n" +
					"\t\"name\":\"TAG_DEF\",\n" +
					"\t\"parentUuid\":\"\",\n" +
					"\t\"systemPropertyDefinitions\":{\n" +
					"\t\t\"CATMA_SYSPROP_DEF\":{\n" +
					"\t\t\t\"name\":\"catma_markupauthor\",\n" +
					"\t\t\t\"possibleValueList\":[\"SYSPROP_VAL_1\",\"SYSPROP_VAL_2\"],\n" +
					"\t\t\t\"uuid\":\"CATMA_SYSPROP_DEF\"\n" +
					"\t\t}\n" +
					"\t},\n" +
					"\t\"tagsetDefinitionUuid\":\"CATMA_TAGSET_DEF\",\n" +
					"\t\"userDefinedPropertyDefinitions\":{\n" +
					"\t\t\"CATMA_UPROP_DEF\":{\n" +
					"\t\t\t\"name\":\"UPROP_DEF\",\n" +
					"\t\t\t\"possibleValueList\":[\"UPROP_VAL_1\",\"UPROP_VAL_2\"],\n" +
					"\t\t\t\"uuid\":\"CATMA_UPROP_DEF\"\n" +
					"\t\t}\n" +
					"\t},\n" +
					"\t\"uuid\":\"CATMA_TAG_DEF\"\n" +
					"}";
			FileUtils.writeStringToFile(
				fakeTagDefinitionPropertyDefsFilePath, fakeSerializedTagDefinition, StandardCharsets.UTF_8
			);

			TagsetHandler tagsetHandler = new TagsetHandler(jGitRepoManager, this.gitLabServerManager);

			TagsetDefinition tagsetDefinition = tagsetHandler.open(
				"CATMA_TAGSET_DEF", "fakeProjectId"
			);

			assertEquals("CATMA_TAGSET_DEF", tagsetDefinition.getUuid());
			assertEquals("TAGSET_DEF", tagsetDefinition.getName());

			assertFalse(tagsetDefinition.isEmpty());

			TagDefinition loadedTagDefinition = tagsetDefinition.getTagDefinition("CATMA_TAG_DEF");

			assertNotNull(loadedTagDefinition);

			assertEquals("CATMA_TAG_DEF", loadedTagDefinition.getUuid());
			assertEquals("TAG_DEF", loadedTagDefinition.getName());
			assertEquals("", loadedTagDefinition.getParentUuid());
		}
	}
}
