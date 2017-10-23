package de.catma.repository.git;

import de.catma.repository.git.exceptions.TagsetHandlerException;
import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManagerTest;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.repository.git.serialization.model_wrappers.GitTagDefinition;
import de.catma.repository.git.serialization.models.TagsetDefinitionHeader;
import de.catma.tag.PropertyDefinition;
import de.catma.tag.PropertyPossibleValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.Version;
import de.catma.util.IDGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
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
	private RemoteGitServerManager remoteGitServerManager;
	private LocalGitRepositoryManager localGitRepositoryManager;
	private TagsetHandler tagsetHandler;

	private ArrayList<String> tagsetReposToDeleteOnTearDown = new ArrayList<>();
	private ArrayList<String> projectsToDeleteOnTearDown = new ArrayList<>();

	private File createdRepositoryPath = null;

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
		// create a fake CATMA user which we'll use to instantiate the RemoteGitServerManager
		de.catma.user.User catmaUser = Randomizer.getDbUser();

		this.remoteGitServerManager = new RemoteGitServerManager(this.catmaProperties, catmaUser);
		this.remoteGitServerManager.replaceGitLabServerUrl = true;

		this.localGitRepositoryManager = new LocalGitRepositoryManager(this.catmaProperties);

		this.tagsetHandler = new TagsetHandler(
			this.localGitRepositoryManager, this.remoteGitServerManager
		);
	}

	@After
	public void tearDown() throws Exception {

		if (this.tagsetReposToDeleteOnTearDown.size() > 0) {
			for (String tagsetId : this.tagsetReposToDeleteOnTearDown) {
				List<Project> projects = this.remoteGitServerManager.getAdminGitLabApi().getProjectApi().getProjects(
						tagsetId
				); // this getProjects overload does a search
				for (Project project : projects) {
					this.remoteGitServerManager.deleteRepository(project.getId());
				}
				await().until(
						() -> this.remoteGitServerManager.getAdminGitLabApi().getProjectApi().getProjects().isEmpty()
				);
			}
			this.tagsetReposToDeleteOnTearDown.clear();
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

			ProjectHandler projectHandler = new ProjectHandler(
					localGitRepoManager, this.remoteGitServerManager
			);

			String projectId = projectHandler.create(
					"Test CATMA Project for Tagset", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);


			String name = "InterestingTagset";
			String description = "Pretty interesting stuff";
			Version version = new Version();

			String tagsetId = tagsetHandler.create(
					name,
					description,
					version,
					projectId);

			assertNotNull(tagsetId);
			assert tagsetId.startsWith("CATMA_");

			// we don't add the tagsetId to this.tagsetReposToDeleteOnTearDown as deletion of the
			// project will take care of that for us

			File expectedRepoPath = new File(localGitRepoManager.getRepositoryBasePath(), tagsetId);
			assert expectedRepoPath.exists();
			assert expectedRepoPath.isDirectory();

			assert Arrays.asList(expectedRepoPath.list()).contains("header.json");

			TagsetDefinitionHeader expectedHeader = new TagsetDefinitionHeader(name, description, version);

			String serialized = FileUtils.readFileToString(new File(expectedRepoPath, "header.json"), StandardCharsets.UTF_8);
			TagsetDefinitionHeader actualHeader = new SerializationHelper<TagsetDefinitionHeader>()
					.deserialize(
							serialized,
							TagsetDefinitionHeader.class
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
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			TagsetHandler tagsetHandler = new TagsetHandler(
					localGitRepoManager, this.remoteGitServerManager
			);

			thrown.expect(TagsetHandlerException.class);
			thrown.expectMessage("Not implemented");
			tagsetHandler.delete("fake");
		}
	}

	@Test
	public void addTagDefinitionWithoutParent() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			ProjectHandler projectHandler = new ProjectHandler(
					localGitRepoManager, this.remoteGitServerManager
			);

			String projectId = projectHandler.create(
					"Test CATMA Project for Tagset", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);


			String name = "InterestingTagset";
			String description = "Pretty interesting stuff";
			Version version = new Version();

			String tagsetId = tagsetHandler.create(
					name,
					description,
					version,
					projectId);

			assertNotNull(tagsetId);

//			Integer id, String uuid,
//					String name, Version version,
//					Integer parentId, String parentUuid

			String tagDefinitionId = this.idGenerator.generate();
			Version tagDefVersion = new Version();

			TagDefinition tagDefinition = new TagDefinition(
					1, tagDefinitionId,
					"FakeTagdefinitionName", tagDefVersion,
					null, null);

			PropertyDefinition propDef = new PropertyDefinition();
			propDef.setId(1);
			propDef.setUuid("CATMA_userPropdefUUID");
			propDef.setName("CunningProperty");
			propDef.setPossibleValueList(new PropertyPossibleValueList("Weather"));
			tagDefinition.addUserDefinedPropertyDefinition(propDef);

			String result = tagsetHandler.addTagDefinition(tagsetId, tagDefinition);

			assertEquals(tagDefinitionId, result);

			String tagDefinitionPath = String.format("%s/%s", tagsetId, tagDefinition.getUuid());

			File expectedTagDefinitionPath = new File(localGitRepoManager.getRepositoryBasePath(), tagDefinitionPath);
			assert expectedTagDefinitionPath.exists() : "Directory does not exist";
			assert expectedTagDefinitionPath.isDirectory() : "Path is not a directory";

			assert Arrays.asList(expectedTagDefinitionPath.list()).contains("propertydefs.json");

			GitTagDefinition expectedGitTagDefinition = new GitTagDefinition(tagDefinition);

			String serialized = FileUtils.readFileToString(new File(expectedTagDefinitionPath, "propertydefs.json"), StandardCharsets.UTF_8);
			GitTagDefinition actualGitTagDefinition = new SerializationHelper<GitTagDefinition>()
					.deserialize(
							serialized,
							GitTagDefinition.class
					);

			assertEquals(expectedGitTagDefinition.getName(), actualGitTagDefinition.getName());
			assertEquals(expectedGitTagDefinition.getParentUuid(), actualGitTagDefinition.getParentUuid());
			assertEquals(expectedGitTagDefinition.getUuid(), actualGitTagDefinition.getUuid());
		}
	}

	@Test
	public void addTagDefinitionWithParent() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(this.catmaProperties)) {
			ProjectHandler projectHandler = new ProjectHandler(
					localGitRepoManager, this.remoteGitServerManager
			);

			String projectId = projectHandler.create(
					"Test CATMA Project for Tagset", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);


			String name = "InterestingTagset";
			String description = "Pretty interesting stuff";
			Version version = new Version();

			String tagsetId = tagsetHandler.create(
					name,
					description,
					version,
					projectId);

			assertNotNull(tagsetId);

//			Integer id, String uuid,
//					String name, Version version,
//					Integer parentId, String parentUuid

			String tagDefinitionId = this.idGenerator.generate();
			String parentTagDefinitionId = this.idGenerator.generate();
			Version tagDefVersion = new Version();

			TagDefinition tagDefinition = new TagDefinition(
					1, tagDefinitionId,
					"FakeTagdefinitionName", tagDefVersion,
					2, parentTagDefinitionId);

			String result = tagsetHandler.addTagDefinition(tagsetId, tagDefinition);

			assertEquals(tagDefinitionId, result);

			String tagDefinitionPath = String.format("%s/%s/%s", tagsetId, parentTagDefinitionId, tagDefinition.getUuid());

			Logger.getLogger(this.getClass().toString()).info(tagDefinitionPath);

			File expectedTagDefinitionPath = new File(localGitRepoManager.getRepositoryBasePath(), tagDefinitionPath);
			assert expectedTagDefinitionPath.exists() : "Directory does not exist";
			assert expectedTagDefinitionPath.isDirectory() : "Path is not a directory";

			assert Arrays.asList(expectedTagDefinitionPath.list()).contains("propertydefs.json");

			assert Arrays.asList(expectedTagDefinitionPath.list()).contains("propertydefs.json");

			GitTagDefinition expectedGitTagDefinition = new GitTagDefinition(tagDefinition);

			String serialized = FileUtils.readFileToString(new File(expectedTagDefinitionPath, "propertydefs.json"), StandardCharsets.UTF_8);
			GitTagDefinition actualGitTagDefinition = new SerializationHelper<GitTagDefinition>()
					.deserialize(
							serialized,
							GitTagDefinition.class
					);

			assertEquals(expectedGitTagDefinition.getName(), actualGitTagDefinition.getName());
			assertEquals(expectedGitTagDefinition.getParentUuid(), actualGitTagDefinition.getParentUuid());
			assertEquals(expectedGitTagDefinition.getUuid(), actualGitTagDefinition.getUuid());
		}
	}

}
