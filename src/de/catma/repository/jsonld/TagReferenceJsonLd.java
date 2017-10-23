package de.catma.repository.jsonld;

import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.git.serialization.SerializationHelper;
import de.catma.tag.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class TagReferenceJsonLd {
	private final Logger logger;
	private TagReference tagReference;

	public TagReferenceJsonLd() {
		this.logger = Logger.getLogger(this.getClass().getName());
	}

	public TagReferenceJsonLd(TagReference tagReference) {
		this.logger = Logger.getLogger(this.getClass().getName());
		this.tagReference = tagReference;
	}

	public TagReference getTagReference() {
		return this.tagReference;
	}

	public TagDefinition findTagDefinitionForTagInstance(String uuid) {
		// TODO: replace with proper calls to the Repo/DB
		Version version = new Version();
		return new TagDefinition(1, uuid, "FAKE_TAG_DEFINITION", version, null, null);
	}

	public TagInstance buildTagInstanceFromJson(TagInstanceLd tagInstanceLd) {
		String tagInstanceUUID = tagInstanceLd.getTagInstanceUUID();
		String tagDefinitionUUID = tagInstanceLd.getBody().getTagDefinitionUUID();

		TagDefinition tagDefinition = this.findTagDefinitionForTagInstance(tagDefinitionUUID);

		TagInstance tagInstance = new TagInstance(tagInstanceUUID, tagDefinition);

		for (Map.Entry<String, String> entry : tagInstanceLd.getBody().getProperties().entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();

			String propertyDefUuid = tagInstanceLd.getBody().getPropertyDefinitionUUID(key);

			Property property = this.buildProperty(propertyDefUuid, value);
			tagInstance.addUserDefinedProperty(property);
		}

		return tagInstance;
	}

	public PropertyDefinition findPropertyDefinitionByUUID(String uuid) {
		// TODO: replace with proper calls to the Repo/DB
		String name = uuid;
		List<String> possibleValues = new ArrayList<>();
		PropertyPossibleValueList propertyPossibleValueList = new PropertyPossibleValueList(
			possibleValues, false
		);

		PropertyDefinition propertyDefinition = new PropertyDefinition(1, uuid, name, propertyPossibleValueList);

		return propertyDefinition;
	}

	public Property buildProperty(String propertyDefUUID, String value) {
		PropertyDefinition propertyDefinition = this.findPropertyDefinitionByUUID(propertyDefUUID);
		PropertyValueList propertyValueList = new PropertyValueList(value);
		return new Property(propertyDefinition, propertyValueList);
	}

	public TagReferenceJsonLd deserialize(InputStream inputStream) throws IOException, URISyntaxException {
		String serializedRepresentation = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
		TagInstanceLd tagInstanceLd = new SerializationHelper<TagInstanceLd>().deserialize(serializedRepresentation, TagInstanceLd.class);

		TagInstance tagInstance = this.buildTagInstanceFromJson(tagInstanceLd);

		Range range = new Range(
			tagInstanceLd.getTarget().getTextPositionSelector().getStart(), tagInstanceLd.getTarget().getTextPositionSelector().getEnd()
		);
		this.tagReference = new TagReference(tagInstance, tagInstanceLd.getTarget().getSource(), range);

		return this;
	}

	public String serialize() {
		TagInstanceLd tagInstanceLd = new TagInstanceLd();
		tagInstanceLd.setContext("http://www.w3.org/ns/anno.jsonld");
		tagInstanceLd.setType("Annotation");
		tagInstanceLd.setId("http://catma.de/portal/annotation/" + this.tagReference.getTagInstance().getUuid());

		tagInstanceLd.setBody(new TagInstanceLdBody());
		tagInstanceLd.getBody().getContext().put("tag", "http://catma.de/portal/tag");
		tagInstanceLd.getBody().setType("Dataset");
		tagInstanceLd.getBody().setTag("http://catma.de/portal/tag/" + this.tagReference.getTagDefinition().getUuid());

		tagInstanceLd.setTarget(new TagInstanceLdTarget());
		tagInstanceLd.getTarget().setSource(this.tagReference.getTarget().toString());
		tagInstanceLd.getTarget().getTextPositionSelector().setStart(this.tagReference.getRange().getStartPoint());
		tagInstanceLd.getTarget().getTextPositionSelector().setEnd(this.tagReference.getRange().getEndPoint());

		String tagDefinitionUuid = this.tagReference.getTagDefinition().getUuid();

		Collection<Property> userDefinedProperties = this.tagReference.getTagInstance().getUserDefinedProperties();

		for (Property property : userDefinedProperties) {
			tagInstanceLd.getBody().getContext().put(
				property.getName(),
				"http://catma.de/portal/tag/" + tagDefinitionUuid + "/property/" +
						property.getPropertyDefinition().getUuid()
			);

			// TODO: Have multiple user defined values per property
			tagInstanceLd.getBody().getProperties().put(property.getName(), property.getPropertyValueList().getFirstValue());
		}

		return new SerializationHelper<TagInstanceLd>().serialize(tagInstanceLd);
	}
}
