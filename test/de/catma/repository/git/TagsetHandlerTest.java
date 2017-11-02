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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;

public class TagsetHandlerTest {
	private Properties catmaProperties;
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
		// create a fake CATMA user which we'll use to instantiate the GitLabServerManager
		de.catma.user.User catmaUser = Randomizer.getDbUser();

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
			try (JGitRepoManager localGitRepoManager = new JGitRepoManager(
					this.catmaProperties, "fakeUserIdentifier"
			)) {
				ProjectHandler projectHandler = new ProjectHandler(localGitRepoManager, this.gitLabServerManager);

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
		try (JGitRepoManager localGitRepoManager = new JGitRepoManager(
				this.catmaProperties, "fakeUserIdentifier"
		)) {

			ProjectHandler projectHandler = new ProjectHandler(localGitRepoManager, this.gitLabServerManager);

			String projectId = projectHandler.create(
				"Test CATMA Project for Tagset", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// the JGitRepoManager instance should always be in a detached state after ProjectHandler
			// calls return
			assertFalse(localGitRepoManager.isAttached());

			TagsetHandler tagsetHandler = new TagsetHandler(localGitRepoManager, this.gitLabServerManager);

			String name = "InterestingTagset";
			String description = "Pretty interesting stuff";
			Version version = new Version();

			String tagsetId = tagsetHandler.create(name, description, version, projectId);
			// we don't add the tagsetId to this.tagsetReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			assertNotNull(tagsetId);
			assert tagsetId.startsWith("CATMA_");

			File expectedRepoPath = new File(
				localGitRepoManager.getRepositoryBasePath(), TagsetHandler.getTagsetRepositoryName(tagsetId)
			);
			assert expectedRepoPath.exists();
			assert expectedRepoPath.isDirectory();

			assert Arrays.asList(expectedRepoPath.list()).contains("header.json");

			TagsetDefinitionHeader expectedHeader = new TagsetDefinitionHeader(name, description, version);

			String serialized = FileUtils.readFileToString(
				new File(expectedRepoPath, "header.json"), StandardCharsets.UTF_8
			);
			TagsetDefinitionHeader actualHeader = new SerializationHelper<TagsetDefinitionHeader>().deserialize(
				serialized, TagsetDefinitionHeader.class
			);

			assertEquals(expectedHeader.getName(), actualHeader.getName());
			assertEquals(expectedHeader.getDescription(), actualHeader.getDescription());
			assertEquals(expectedHeader.getVersion(), actualHeader.getVersion());
		}
	}

	// how to test for exceptions: https://stackoverflow.com/a/31826781
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void delete() throws Exception {
		try (JGitRepoManager localGitRepoManager = new JGitRepoManager(
				this.catmaProperties, "fakeUserIdentifier"
		)) {

			TagsetHandler tagsetHandler = new TagsetHandler(localGitRepoManager, this.gitLabServerManager);

			thrown.expect(TagsetHandlerException.class);
			thrown.expectMessage("Not implemented");
			tagsetHandler.delete("fake");
		}
	}

	@Test
	public void addTagDefinitionWithoutParent() throws Exception {
		try (JGitRepoManager localGitRepoManager = new JGitRepoManager(
				this.catmaProperties, "fakeUserIdentifier"
		)) {

			ProjectHandler projectHandler = new ProjectHandler(localGitRepoManager, this.gitLabServerManager);

			String projectId = projectHandler.create(
				"Test CATMA Project for Tagset", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			TagsetHandler tagsetHandler = new TagsetHandler(localGitRepoManager, this.gitLabServerManager);

			String name = "InterestingTagset";
			String description = "Pretty interesting stuff";
			Version version = new Version();

			String tagsetId = tagsetHandler.create(name, description, version, projectId);
			// we don't add the tagsetId to this.tagsetReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			assertNotNull(tagsetId);

			String tagDefinitionId = this.idGenerator.generate();
			Version tagDefVersion = new Version();

			TagDefinition tagDefinition = new TagDefinition(
				null, tagDefinitionId, "FakeTagDefinitionName", tagDefVersion, null, null
			);

			PropertyDefinition propDef = new PropertyDefinition(
				null, "CATMA_userPropdefUUID", "CunningProperty",
				new PropertyPossibleValueList("Weather")
			);
			tagDefinition.addUserDefinedPropertyDefinition(propDef);

			String result = tagsetHandler.addTagDefinition(tagsetId, tagDefinition);

			assertEquals(tagDefinitionId, result);

			String tagDefinitionPath = String.format(
				"%s/%s",
				TagsetHandler.getTagsetRepositoryName(tagsetId),
				tagDefinition.getUuid()
			);

			File expectedTagDefinitionPath = new File(localGitRepoManager.getRepositoryBasePath(), tagDefinitionPath);
			assert expectedTagDefinitionPath.exists() : "Directory does not exist";
			assert expectedTagDefinitionPath.isDirectory() : "Path is not a directory";

			assert Arrays.asList(expectedTagDefinitionPath.list()).contains("propertydefs.json");

			GitTagDefinition expectedGitTagDefinition = new GitTagDefinition(tagDefinition);

			String serialized = FileUtils.readFileToString(
				new File(expectedTagDefinitionPath, "propertydefs.json"), StandardCharsets.UTF_8
			);
			GitTagDefinition actualGitTagDefinition = new SerializationHelper<GitTagDefinition>().deserialize(
				serialized, GitTagDefinition.class
			);

			assertEquals(expectedGitTagDefinition.getName(), actualGitTagDefinition.getName());
			assertEquals(expectedGitTagDefinition.getParentUuid(), actualGitTagDefinition.getParentUuid());
			assertEquals(expectedGitTagDefinition.getUuid(), actualGitTagDefinition.getUuid());
		}
	}

	@Test
	public void addTagDefinitionWithParent() throws Exception {
		try (JGitRepoManager localGitRepoManager = new JGitRepoManager(
				this.catmaProperties, "fakeUserIdentifier"
		)) {

			ProjectHandler projectHandler = new ProjectHandler(
				localGitRepoManager, this.gitLabServerManager
			);

			String projectId = projectHandler.create(
				"Test CATMA Project for Tagset", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			TagsetHandler tagsetHandler = new TagsetHandler(localGitRepoManager, this.gitLabServerManager);

			String name = "InterestingTagset";
			String description = "Pretty interesting stuff";
			Version version = new Version();

			String tagsetId = tagsetHandler.create(name, description, version, projectId);

			assertNotNull(tagsetId);

			String tagDefinitionId = this.idGenerator.generate();
			String parentTagDefinitionId = this.idGenerator.generate();
			Version tagDefVersion = new Version();

			TagDefinition tagDefinition = new TagDefinition(
				null, tagDefinitionId, "FakeTagdefinitionName", tagDefVersion,
				null, parentTagDefinitionId
			);

			String result = tagsetHandler.addTagDefinition(tagsetId, tagDefinition);

			assertEquals(tagDefinitionId, result);

			String tagDefinitionPath = String.format(
				"%s/%s/%s",
				TagsetHandler.getTagsetRepositoryName(tagsetId),
				parentTagDefinitionId,
				tagDefinition.getUuid()
			);

			Logger.getLogger(this.getClass().toString()).info(tagDefinitionPath);

			File expectedTagDefinitionPath = new File(localGitRepoManager.getRepositoryBasePath(), tagDefinitionPath);
			assert expectedTagDefinitionPath.exists() : "Directory does not exist";
			assert expectedTagDefinitionPath.isDirectory() : "Path is not a directory";

			assert Arrays.asList(expectedTagDefinitionPath.list()).contains("propertydefs.json");

			GitTagDefinition expectedGitTagDefinition = new GitTagDefinition(tagDefinition);

			String serialized = FileUtils.readFileToString(
				new File(expectedTagDefinitionPath, "propertydefs.json"), StandardCharsets.UTF_8
			);
			GitTagDefinition actualGitTagDefinition = new SerializationHelper<GitTagDefinition>().deserialize(
				serialized, GitTagDefinition.class
			);

			assertEquals(expectedGitTagDefinition.getName(), actualGitTagDefinition.getName());
			assertEquals(expectedGitTagDefinition.getParentUuid(), actualGitTagDefinition.getParentUuid());
			assertEquals(expectedGitTagDefinition.getUuid(), actualGitTagDefinition.getUuid());
		}
	}

	@Test
	public void open() throws Exception {
		try (JGitRepoManager localGitRepoManager = new JGitRepoManager(
				this.catmaProperties,"fakeUserIdentifier"
		)) {
			// TODO: use JsonLdWebAnnotationTest.getTagInstance once it's been implemented
			// for now, we need to create a fake project repo with fake submodules to make this test pass
			File fakeProjectPath = new File(localGitRepoManager.getRepositoryBasePath(), "fakeProjectId_corpus");
			// need to init the fake project repo, otherwise JGitRepoManager will fail to open it later
			localGitRepoManager.init(fakeProjectPath.getName(), null);
			localGitRepoManager.detach();  // can't call open on an attached instance
			this.directoriesToDeleteOnTearDown.add(fakeProjectPath);

			File fakeTagsetSubmodulePath = new File(fakeProjectPath, "tagsets/CATMA_TAGSET_DEF_tagset");

			File fakeTagsetHeaderFilePath = new File(fakeTagsetSubmodulePath, "header.json");
			String fakeSerializedTagsetHeader = "" +
					"{\n" +
					"\t\"description\":\"\",\n" +
					"\t\"name\":\"TAGSET_DEF\",\n" +
					"\t\"version\":\"2017-10-31T14:40:00+0200\"\n" +
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

			TagsetHandler tagsetHandler = new TagsetHandler(localGitRepoManager, this.gitLabServerManager);

			TagsetDefinition tagsetDefinition = tagsetHandler.open(
				"CATMA_TAGSET_DEF", "fakeProjectId"
			);

			assertEquals("CATMA_TAGSET_DEF", tagsetDefinition.getUuid());
			assertEquals("TAGSET_DEF", tagsetDefinition.getName());
			assertEquals("2017-10-31T14:40:00.000+0200", tagsetDefinition.getVersion().toString());

			assertFalse(tagsetDefinition.isEmpty());

			TagDefinition loadedTagDefinition = tagsetDefinition.getTagDefinition("CATMA_TAG_DEF");

			assertNotNull(loadedTagDefinition);

			assertEquals("CATMA_TAG_DEF", loadedTagDefinition.getUuid());
			assertEquals("TAG_DEF", loadedTagDefinition.getName());
			assertEquals("", loadedTagDefinition.getParentUuid());
		}
	}
}
