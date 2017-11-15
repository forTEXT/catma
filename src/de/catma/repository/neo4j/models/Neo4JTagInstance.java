package de.catma.repository.neo4j.models;

import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.neo4j.exceptions.Neo4JUserMarkupCollectionException;
import de.catma.repository.neo4j.serialization.model_wrappers.Neo4JProperty;
import de.catma.repository.neo4j.serialization.model_wrappers.Neo4JRange;
import de.catma.repository.neo4j.serialization.model_wrappers.Neo4JTagDefinition;
import de.catma.tag.Property;
import de.catma.tag.PropertyValueList;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

@NodeEntity(label="TagInstance")
public class Neo4JTagInstance {
	@Id
	private String uuid;

	@Relationship(type="HAS_TAG_DEFINITION", direction=Relationship.OUTGOING)
	private Neo4JTagDefinition tagDefinition;

	@Relationship(type="HAS_SYSTEM_PROPERTY", direction=Relationship.OUTGOING)
	private List<Neo4JProperty> systemProperties;

	@Relationship(type="HAS_USER_DEFINED_PROPERTY", direction=Relationship.OUTGOING)
	private List<Neo4JProperty> userDefinedProperties;

	@Relationship(type="HAS_RANGE", direction=Relationship.OUTGOING)
	private List<Neo4JRange> ranges;

	private String userMarkupCollectionUuid;
	private String target;

	public Neo4JTagInstance() {
		this.systemProperties = new ArrayList<>();
		this.userDefinedProperties = new ArrayList<>();
		this.ranges = new ArrayList<>();
	}

	public Neo4JTagInstance(List<TagReference> tagReferences) throws Neo4JUserMarkupCollectionException {
		this();

		this.setTagReferences(tagReferences);
	}

	public List<TagReference> getTagReferences() throws Neo4JUserMarkupCollectionException {
		List<Property> userDefinedProperties = this.userDefinedProperties.stream().map(Neo4JProperty::getProperty)
				.collect(Collectors.toList());

		List<Property> systemProperties = this.systemProperties.stream().map(Neo4JProperty::getProperty)
				.collect(Collectors.toList());

		List<Range> ranges = this.ranges.stream().map(Neo4JRange::getRange).collect(Collectors.toList());

		TagDefinition tagDefinition = this.tagDefinition.getTagDefinition();

		TagInstance tagInstance = new TagInstance(this.uuid, tagDefinition);
		// the TagInstance constructor sets default values for system properties, so we need to clear them
		for (Property property : tagInstance.getSystemProperties()) {
			property.setPropertyValueList(new PropertyValueList());
		}

		userDefinedProperties.forEach(tagInstance::addUserDefinedProperty);
		systemProperties.forEach(tagInstance::addSystemProperty);

		// TODO: figure out how to do this with .stream().map while handling exceptions properly
		// see https://stackoverflow.com/a/33218789 & https://stackoverflow.com/a/30118121 for pointers
		List<TagReference> tagReferences = new ArrayList<>();
		try {
			for (Range range : ranges) {
				tagReferences.add(new TagReference(tagInstance, this.target, range, this.userMarkupCollectionUuid));
			}
		}
		catch (URISyntaxException e) {
			throw new Neo4JUserMarkupCollectionException(
					"Failed to turn internal representation back into a collection of TagReference objects", e
			);
		}

		return tagReferences;
	}

	public void setTagReferences(List<TagReference> tagReferences) throws Neo4JUserMarkupCollectionException {
		// TODO: possibly some or all of this should happen in the constructor directly

		// assert that all TagReference objects are for the same TagInstance and thus share the same TagDefinition and
		// properties
		Set<TagInstance> uniqueTagInstances = new HashSet<>(tagReferences.stream().map(TagReference::getTagInstance)
				.collect(Collectors.toSet()));
		if (uniqueTagInstances.size() > 1) {
			throw new Neo4JUserMarkupCollectionException(
					"Supplied TagReference objects are not all for the same TagInstance"
			);
		}

		this.uuid = tagReferences.get(0).getTagInstance().getUuid();
		this.tagDefinition = new Neo4JTagDefinition(tagReferences.get(0).getTagDefinition(), null);

		this.systemProperties.clear();
		for (Property property : tagReferences.get(0).getTagInstance().getSystemProperties()) {
			this.systemProperties.add(new Neo4JProperty(property));
		}

		this.userDefinedProperties.clear();
		for (Property property : tagReferences.get(0).getTagInstance().getUserDefinedProperties()) {
			this.userDefinedProperties.add(new Neo4JProperty(property));
		}

		this.ranges = new ArrayList<>(tagReferences.stream().map(Neo4JRange::new).collect(Collectors.toList()));

		this.userMarkupCollectionUuid = tagReferences.get(0).getUserMarkupCollectionUuid();
		this.target = tagReferences.get(0).getTarget().toString();
	}
}
