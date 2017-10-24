package de.catma.repository.git.serialization.models.json_ld;

import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.git.exceptions.JsonLdWebAnnotationException;
import de.catma.repository.git.exceptions.SourceDocumentHandlerException;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.tag.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

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

	@Test
	public void serialize() throws Exception {
		String sourceDocumentUri = "http://catma.de/portal/sourcedocument/CATMA_SOURCEDOC";

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

		Range range1 = new Range(12, 18);
		Range range2 = new Range(41, 47);

		List<TagReference> tagReferences = new ArrayList<TagReference>(
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

	// how to test for exceptions: https://stackoverflow.com/a/31826781
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void toTagReferenceList() throws Exception {
		JsonLdWebAnnotation jsonLdWebAnnotation = new SerializationHelper<JsonLdWebAnnotation>().deserialize(
				this.expectedSerializedRepresentation, JsonLdWebAnnotation.class
		);

		assertNotNull(jsonLdWebAnnotation);

		thrown.expect(JsonLdWebAnnotationException.class);
		thrown.expectMessage("Not implemented");
		jsonLdWebAnnotation.toTagReferenceList();
	}
}
