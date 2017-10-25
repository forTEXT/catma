package de.catma.repository.git.serialization.models.json_ld;

import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.tag.*;
import mockit.Expectations;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(JMockit.class)
public class JsonLdWebAnnotationTest {
	private final String expectedSerializedRepresentation = "" +
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

	private TagInstance getFakeTagInstance() {
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
			1, "CATMA_TAG_DEF", "TAG_DEF", new Version(), null, null
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

	@Test
	public void serialize() throws Exception {
		TagInstance tagInstance = this.getFakeTagInstance();

		String sourceDocumentUri = "http://catma.de/portal/sourcedocument/CATMA_SOURCEDOC";

		Range range1 = new Range(12, 18);
		Range range2 = new Range(41, 47);

		List<TagReference> tagReferences = new ArrayList<>(
			Arrays.asList(
				new TagReference(tagInstance, sourceDocumentUri, range1),
				new TagReference(tagInstance, sourceDocumentUri, range2)
			)
		);

		JsonLdWebAnnotation jsonLdWebAnnotation = new JsonLdWebAnnotation(tagReferences);

		String serialized = new SerializationHelper<JsonLdWebAnnotation>().serialize(jsonLdWebAnnotation);

		assert this.expectedSerializedRepresentation.replaceAll("[\n\t]", "").equals(serialized);
	}

	@Test
	public void deserialize() throws Exception {
		JsonLdWebAnnotation jsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>().deserialize(
			this.expectedSerializedRepresentation, JsonLdWebAnnotation.class
		);

		assertNotNull(jsonLdWebAnnotation);

		// re-serialize and assert that what comes out is what went in
		String serialized = new SerializationHelper<JsonLdWebAnnotation>().serialize(jsonLdWebAnnotation);

		assert this.expectedSerializedRepresentation.replaceAll("[\n\t]", "").equals(serialized);
	}

	@Test
	public void toTagReferenceList() throws Exception {
		JsonLdWebAnnotation jsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>().deserialize(
			this.expectedSerializedRepresentation, JsonLdWebAnnotation.class
		);

		assertNotNull(jsonLdWebAnnotation);

		// mock out the getTagInstance method on JsonLdWebAnnotation
		// TODO: stop mocking once implemented
		new Expectations(jsonLdWebAnnotation) {{
			jsonLdWebAnnotation.getTagInstance(); result = getFakeTagInstance();
		}};

		List<TagReference> tagReferences = jsonLdWebAnnotation.toTagReferenceList();

		new Verifications() {{
			jsonLdWebAnnotation.getTagInstance();
		}};

		assertEquals(2, tagReferences.size());

		for (TagReference tagReference : tagReferences) {
			TagDefinition tagDefinition = tagReference.getTagDefinition();
			TagInstance tagInstance = tagReference.getTagInstance();

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
			new URI("http://catma.de/portal/sourcedocument/CATMA_SOURCEDOC"), tagReferences.get(0).getTarget()
		);
		assertEquals(new Range(12, 18), tagReferences.get(0).getRange());

		assertEquals(
			new URI("http://catma.de/portal/sourcedocument/CATMA_SOURCEDOC"), tagReferences.get(1).getTarget()
		);
		assertEquals(new Range(41, 47), tagReferences.get(1).getRange());
	}
}
