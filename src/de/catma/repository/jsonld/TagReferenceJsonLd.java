package de.catma.repository.jsonld;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.tag.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



public class TagReferenceJsonLd {

	private final Logger logger;
	private TagReference tagReference;

	public TagReferenceJsonLd(){
		this.logger = Logger.getLogger(this.getClass().getName());
	}

	public TagReferenceJsonLd(TagReference tagReference){
		this.logger = Logger.getLogger(this.getClass().getName());

		this.tagReference = tagReference;
	}

	public TagReference getTagReference(){
		return this.tagReference;
	}

	public TagDefinition findTagDefinitionForTagInstance(String uuid){
		//TODO: replace with proper calls to the Repo/DB
		Version version = new Version();
		return new TagDefinition(1, uuid, "FAKE_TAG_DEFINITION", version, null, null);
	}

	public TagInstance buildTagInstanceFromJson(TagInstanceLd tagInstanceLd){
		String tagInstanceUUID = tagInstanceLd.getTagInstanceUuid();
		String tagDefinitionUUID = tagInstanceLd.body.getTagDefinitionUuid();

		TagDefinition tagDefinition = this.findTagDefinitionForTagInstance(tagDefinitionUUID);

		TagInstance tagInstance = new TagInstance(tagInstanceUUID, tagDefinition);

		for (Map.Entry<String, String> entry : tagInstanceLd.body.properties.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();

			String propertyDefUuid = tagInstanceLd.body.getPropertyDefinitionUuid(key);

			Property property = this.BuildProperty(propertyDefUuid, value);
			tagInstance.addUserDefinedProperty(property);
		}

		return tagInstance;
	}

	public PropertyDefinition FindPropertyDefinitionFromUUID(String uuid){
		//TODO: replace with proper calls to the Repo/DB

		String name = uuid;
		List<String> possibleValues = new ArrayList<>();
		PropertyPossibleValueList propertyPossibleValueList = new PropertyPossibleValueList(possibleValues, false);

		PropertyDefinition propertyDefinition = new PropertyDefinition(1, uuid, name, propertyPossibleValueList);

		return propertyDefinition;
	}

	public Property BuildProperty(String propertyDefUUID, String value){
		PropertyDefinition propertyDefinition = this.FindPropertyDefinitionFromUUID(propertyDefUUID);
		PropertyValueList propertyValueList = new PropertyValueList(value);

		return new Property(propertyDefinition, propertyValueList);
	}

	public TagReferenceJsonLd Deserialize (InputStream inputStream) throws IOException, URISyntaxException {
		JsonIterator iter = JsonIterator.parse(inputStream, 128);

		TagInstanceLd tagInstanceLd = iter.read(TagInstanceLd.class);
		iter.close();

		TagInstance tagInstance = this.buildTagInstanceFromJson(tagInstanceLd);

		Range range = new Range(tagInstanceLd.target.TextPositionSelector.start, tagInstanceLd.target.TextPositionSelector.end);
		this.tagReference = new TagReference(tagInstance, tagInstanceLd.target.source, range);

		return this;
	}

	public String Serialize(){
		TagInstanceLd tagInstanceLd = new TagInstanceLd();
		tagInstanceLd.context = "http://www.w3.org/ns/anno.jsonld";
		tagInstanceLd.type = "Annotation";
		tagInstanceLd.id = "http://catma.de/portal/annotation/" + this.tagReference.getTagInstance().getUuid();

		tagInstanceLd.body = new TagInstanceLdBody();
		tagInstanceLd.body.context.put("tag", "http://catma.de/portal/tag");

		tagInstanceLd.body.type = "Dataset";
		tagInstanceLd.body.tag = "http://catma.de/portal/tag/" + this.tagReference.getTagDefinition().getUuid();

		tagInstanceLd.target = new TagInstanceLdTarget();
		tagInstanceLd.target.source = this.tagReference.getTarget().toString();
		tagInstanceLd.target.TextPositionSelector.start = this.tagReference.getRange().getStartPoint();
		tagInstanceLd.target.TextPositionSelector.end = this.tagReference.getRange().getEndPoint();

		String tagDefinitionUuid = this.tagReference.getTagDefinition().getUuid();
		Collection<Property> userDefinedProperties = this.tagReference.getTagInstance().getUserDefinedProperties();
		for (Property property : userDefinedProperties) {
			tagInstanceLd.body.context.put(property.getName(), "http://catma.de/portal/tag/" + tagDefinitionUuid + "/property/" + property.getPropertyDefinition().getUuid());

			// TODO: Have multiple user defined values per property
			tagInstanceLd.body.properties.put(property.getName(), property.getPropertyValueList().getFirstValue());
		}

		return JsonStream.serialize(tagInstanceLd);
	}
}
