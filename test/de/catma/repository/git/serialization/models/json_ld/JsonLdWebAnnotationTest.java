package de.catma.repository.git.serialization.models.json_ld;

import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.git.ProjectHandler;
import de.catma.repository.git.managers.LocalGitRepositoryManager;
import de.catma.repository.git.managers.RemoteGitServerManager;
import de.catma.repository.git.managers.RemoteGitServerManagerTest;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.tag.*;
import helpers.Randomizer;
import org.gitlab4j.api.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.net.URI;
import java.util.*;

import static org.junit.Assert.*;

public class JsonLdWebAnnotationTest {
	private final String expectedSerializedRepresentation = "" +
			"{\n" +
			"\t\"body\":{\n" +
			"\t\t\"@context\":{\n" +
			"\t\t\t\"SYSPROP_DEF\":\"http://catma.de/gitlab/fakeProjectId_corpus/tagsets/CATMA_TAGSET_DEF_tagset/CATMA_TAG_DEF/propertydefs.json/CATMA_SYSPROP_DEF\",\n" +
			"\t\t\t\"UPROP_DEF\":\"http://catma.de/gitlab/fakeProjectId_corpus/tagsets/CATMA_TAGSET_DEF_tagset/CATMA_TAG_DEF/propertydefs.json/CATMA_UPROP_DEF\",\n" +
			"\t\t\t\"tag\":\"http://catma.de/portal/tag\",\n" +
			"\t\t\t\"tagset\":\"http://catma.de/portal/tagset\"\n" +
			"\t\t},\n" +
			"\t\t\"properties\":{\n" +
			"\t\t\t\"SYSPROP_DEF\":[\"SYSPROP_VAL_1\"],\n" +
			"\t\t\t\"UPROP_DEF\":[\"UPROP_VAL_2\"]\n" +
			"\t\t},\n" +
			"\t\t\"tag\":\"http://catma.de/gitlab/fakeProjectId_corpus/tagsets/CATMA_TAGSET_DEF_tagset/CATMA_TAG_DEF\",\n" +
			"\t\t\"tagset\":\"http://catma.de/gitlab/fakeProjectId_corpus/tagsets/CATMA_TAGSET_DEF_tagset\",\n" +
			"\t\t\"type\":\"Dataset\"\n" +
			"\t},\n" +
			"\t\"@context\":\"http://www.w3.org/ns/anno.jsonld\",\n" +
			"\t\"id\":\"http://catma.de/gitlab/fakeProjectId_corpus/collections/fakeUserMarkupCollectionUuid_markupcollection/annotations/CATMA_TAG_INST.json\",\n" +
			"\t\"target\":{\n" +
			"\t\t\"items\":[{\n" +
			"\t\t\t\"selector\":{\n" +
			"\t\t\t\t\"end\":18,\n" +
			"\t\t\t\t\"start\":12,\n" +
			"\t\t\t\t\"type\":\"TextPositionSelector\"\n" +
			"\t\t\t},\n" +
			"\t\t\t\"source\":\"http://catma.de/gitlab/fakeProjectId_corpus/documents/CATMA_SOURCEDOC_sourcedocument\"\n" +
			"\t\t},\n" +
			"\t\t{\n" +
			"\t\t\t\"selector\":{\n" +
			"\t\t\t\t\"end\":47,\n" +
			"\t\t\t\t\"start\":41,\n" +
			"\t\t\t\t\"type\":\"TextPositionSelector\"\n" +
			"\t\t\t},\n" +
			"\t\t\t\"source\":\"http://catma.de/gitlab/fakeProjectId_corpus/documents/CATMA_SOURCEDOC_sourcedocument\"\n" +
			"\t\t}],\n" +
			"\t\t\"type\":\"List\"\n" +
			"\t},\n" +
			"\t\"type\":\"Annotation\"\n" +
			"}";

	private Properties catmaProperties;
	private RemoteGitServerManager remoteGitServerManager;

	private ArrayList<String> projectsToDeleteOnTearDown = new ArrayList<>();

	public JsonLdWebAnnotationTest() throws Exception {
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
			try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(
					this.catmaProperties, "fakeUserIdentifier")) {
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

	public static TagInstance getFakeTagInstance() {
		PropertyPossibleValueList systemPropertyPossibleValues = new PropertyPossibleValueList(
			Arrays.asList("SYSPROP_VAL_1", "SYSPROP_VAL_2"), true
		);
		PropertyDefinition systemPropertyDefinition = new PropertyDefinition(
			1, "CATMA_SYSPROP_DEF", "SYSPROP_DEF", systemPropertyPossibleValues
		);

		PropertyPossibleValueList userPropertyPossibleValues = new PropertyPossibleValueList(
			Arrays.asList("UPROP_VAL_1", "UPROP_VAL_2"), true
		);
		PropertyDefinition userPropertyDefinition = new PropertyDefinition(
			2, "CATMA_UPROP_DEF", "UPROP_DEF", userPropertyPossibleValues
		);

		TagDefinition tagDefinition = new TagDefinition(
			1, "CATMA_TAG_DEF", "TAG_DEF", new Version(), null, null,
			"CATMA_TAGSET_DEF"
		);
		tagDefinition.addSystemPropertyDefinition(systemPropertyDefinition);
		tagDefinition.addUserDefinedPropertyDefinition(userPropertyDefinition);

		Property systemProperty = new Property(systemPropertyDefinition, new PropertyValueList("SYSPROP_VAL_1"));
		Property userProperty = new Property(userPropertyDefinition, new PropertyValueList("UPROP_VAL_2"));

		TagInstance tagInstance = new TagInstance("CATMA_TAG_INST", tagDefinition);
		tagInstance.addSystemProperty(systemProperty);
		tagInstance.addUserDefinedProperty(userProperty);

		return tagInstance;
	}

	/**
	 * @return a HashMap<String, Object> with a key 'tagInstance' for the TagInstance object and the following
	 *         additional keys which are to be used when formatting the expectedSerializedRepresentation string:
	 *         projectRootRepositoryName, tagsetRepositoryName, tagDefinitionUuid, systemPropertyDefinitionUuid,
	 *         userPropertyDefinitionUuid, markupCollectionRepositoryName, tagInstanceUuid, sourceDocumentRepositoryName
	 */
	public HashMap<String, Object> getTagInstance() throws Exception {
		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(
				this.catmaProperties, "fakeUserIdentifier")) {
			ProjectHandler projectHandler = new ProjectHandler(localGitRepoManager, this.remoteGitServerManager);

			String projectId = projectHandler.create(
					"Test CATMA Project", "This is a test CATMA project"
			);
			this.projectsToDeleteOnTearDown.add(projectId);

			// TODO: complete this once all of the necessary ProjectHandler methods exist
			// add new tagset to project
			// add new source document to project
			// add new markup collection to project

			// create TagInstance object
			// use the real TagInstance object in the toTagReferenceList test to create real TagReference objects, which
			// are then used to construct a JsonLdWebAnnotation object
		}

		return new HashMap<>();
	}

	@Test
	public void serialize() throws Exception {
		TagInstance tagInstance = JsonLdWebAnnotationTest.getFakeTagInstance();

		String sourceDocumentUri = "http://catma.de/gitlab/fakeProjectId_corpus/documents/CATMA_SOURCEDOC_sourcedocument";

		Range range1 = new Range(12, 18);
		Range range2 = new Range(41, 47);

		List<TagReference> tagReferences = new ArrayList<>(
			Arrays.asList(
				new TagReference(
					tagInstance, sourceDocumentUri, range1, "fakeUserMarkupCollectionUuid"
				),
				new TagReference(
					tagInstance, sourceDocumentUri, range2, "fakeUserMarkupCollectionUuid"
				)
			)
		);

		JsonLdWebAnnotation jsonLdWebAnnotation = new JsonLdWebAnnotation(
			"http://catma.de/gitlab", "fakeProjectId", tagReferences
		);

		String serialized = new SerializationHelper<JsonLdWebAnnotation>().serialize(jsonLdWebAnnotation);

		assertEquals(this.expectedSerializedRepresentation.replaceAll("[\n\t]", ""), serialized);
	}

	@Test
	public void deserialize() throws Exception {
		JsonLdWebAnnotation jsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>().deserialize(
			this.expectedSerializedRepresentation, JsonLdWebAnnotation.class
		);

		assertNotNull(jsonLdWebAnnotation);

		// re-serialize and assert that what comes out is what went in
		String serialized = new SerializationHelper<JsonLdWebAnnotation>().serialize(jsonLdWebAnnotation);

		assertEquals(this.expectedSerializedRepresentation.replaceAll("[\n\t]", ""), serialized);
	}

	@Test
	public void toTagReferenceList() throws Exception {
		// TODO: use getTagInstance once it's been implemented
		// TODO: test with a hierarchy of tag definitions

		try (LocalGitRepositoryManager localGitRepoManager = new LocalGitRepositoryManager(
				this.catmaProperties, "fakeUserIdentifier"
		)) {
			JsonLdWebAnnotation jsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>().deserialize(
				this.expectedSerializedRepresentation, JsonLdWebAnnotation.class
			);

			assertNotNull(jsonLdWebAnnotation);

			List<TagReference> tagReferences = jsonLdWebAnnotation.toTagReferenceList(
				"fakeProjectId", "fakeMarkupCollectionId",
				localGitRepoManager, this.remoteGitServerManager
			);

			assertEquals(2, tagReferences.size());

			for (TagReference tagReference : tagReferences) {
				TagDefinition tagDefinition = tagReference.getTagDefinition();
				TagInstance tagInstance = tagReference.getTagInstance();

				assertEquals("CATMA_TAGSET_DEF", tagDefinition.getTagsetDefinitionUuid());
				assertEquals("CATMA_TAG_DEF", tagDefinition.getUuid());
				assertEquals("TAG_DEF", tagDefinition.getName());
				assertEquals("", tagDefinition.getParentUuid());

				PropertyDefinition[] systemPropertyDefinitions = tagDefinition.getSystemPropertyDefinitions()
						.toArray(new PropertyDefinition[0]);
				assertEquals(1, systemPropertyDefinitions.length);
				assertEquals("CATMA_SYSPROP_DEF", systemPropertyDefinitions[0].getUuid());
				assertEquals("SYSPROP_DEF", systemPropertyDefinitions[0].getName());
				List<String> possibleSystemPropertyValues = systemPropertyDefinitions[0].getPossibleValueList()
						.getPropertyValueList().getValues();
				assertEquals(2, possibleSystemPropertyValues.size());
				assertArrayEquals(
					new String[]{"SYSPROP_VAL_1", "SYSPROP_VAL_2"}, possibleSystemPropertyValues.toArray(new String[0])
				);

				PropertyDefinition[] userPropertyDefinitions = tagDefinition.getUserDefinedPropertyDefinitions()
						.toArray(new PropertyDefinition[0]);
				assertEquals(1, userPropertyDefinitions.length);
				assertEquals("CATMA_UPROP_DEF", userPropertyDefinitions[0].getUuid());
				assertEquals("UPROP_DEF", userPropertyDefinitions[0].getName());
				List<String> possibleUserPropertyValues = userPropertyDefinitions[0].getPossibleValueList()
						.getPropertyValueList().getValues();
				assertEquals(2, possibleUserPropertyValues.size());
				assertArrayEquals(
					new String[]{"UPROP_VAL_1", "UPROP_VAL_2"}, possibleUserPropertyValues.toArray(new String[0])
				);

				assertEquals("CATMA_TAG_INST", tagInstance.getUuid());

				Property[] systemProperties = tagInstance.getSystemProperties().toArray(new Property[0]);
				assertEquals(1, systemProperties.length);
				assertEquals(systemPropertyDefinitions[0], systemProperties[0].getPropertyDefinition());
				List<String> systemPropertyValues = systemProperties[0].getPropertyValueList().getValues();
				assertEquals(1, systemPropertyValues.size());
				assertEquals("SYSPROP_VAL_1", systemPropertyValues.get(0));

				Property[] userProperties = tagInstance.getUserDefinedProperties().toArray(new Property[0]);
				assertEquals(1, userProperties.length);
				assertEquals(userPropertyDefinitions[0], userProperties[0].getPropertyDefinition());
				List<String> userPropertyValues = userProperties[0].getPropertyValueList().getValues();
				assertEquals(1, userPropertyValues.size());
				assertEquals("UPROP_VAL_2", userPropertyValues.get(0));
			}

			assertEquals(
				new URI("http://catma.de/gitlab/fakeProjectId_corpus/documents/CATMA_SOURCEDOC"),
				tagReferences.get(0).getTarget()
			);
			assertEquals(new Range(12, 18), tagReferences.get(0).getRange());

			assertEquals(
				new URI("http://catma.de/gitlab/fakeProjectId_corpus/documents/CATMA_SOURCEDOC"),
				tagReferences.get(1).getTarget()
			);
			assertEquals(new Range(41, 47), tagReferences.get(1).getRange());
		}
	}
}
