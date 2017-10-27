package de.catma.repository.git.serialization.models.json_ld;

import com.jsoniter.annotation.JsonProperty;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.git.exceptions.JsonLdWebAnnotationException;
import de.catma.tag.Property;
import de.catma.tag.TagInstance;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a Web Annotation Data Model conformant annotation body of type 'Dataset'.
 *
 * @see <a href="https://www.w3.org/TR/annotation-model/#classes">Web Annotation Data Model - Bodies and Targets - Classes</a>
 */
public class JsonLdWebAnnotationBody_Dataset {
	// we use the TreeMap and TreeSet types so that we get automatic sorting
	private TreeMap<String, String> context;
	private String tagset;
	private String tag;
	private TreeMap<String, TreeSet<String>> properties;

	public JsonLdWebAnnotationBody_Dataset() {
		this.context = new TreeMap<>();
		this.properties = new TreeMap<>();
	}

	public JsonLdWebAnnotationBody_Dataset(List<TagReference> tagReferences) throws JsonLdWebAnnotationException {
		this();
		this.context.put("tagset", "http://catma.de/portal/tagset");  // TODO: what should this URL be?
		this.context.put("tag", "http://catma.de/portal/tag");  // TODO: what should this URL be?

		// assert that all TagReference objects are for the same TagInstance and thus share the same TagDefinition and
		// properties
		Set<TagInstance> uniqueTagInstances = new HashSet<>(tagReferences.stream().map(TagReference::getTagInstance)
				.collect(Collectors.toSet()));
		if (uniqueTagInstances.size() > 1) {
			throw new JsonLdWebAnnotationException(
				"Supplied TagReference objects are not all for the same TagInstance"
			);
		}

		String tagsetDefinitionUuid = tagReferences.get(0).getTagDefinition().getTagsetDefinitionUuid();
		String tagDefinitionUuid = tagReferences.get(0).getTagDefinition().getUuid();
		TagInstance tagInstance = tagReferences.get(0).getTagInstance();

		this.tagset = String.format("http://catma.de/portal/tagset/%s", tagsetDefinitionUuid);  // TODO: actual GitLab URL
		this.tag = String.format("http://catma.de/portal/tag/%s", tagDefinitionUuid);  // TODO: actual GitLab URL

		this.addProperties(tagInstance.getUserDefinedProperties(), tagDefinitionUuid);
		this.addProperties(tagInstance.getSystemProperties(), tagDefinitionUuid);
	}

	private void addProperties(Collection<Property> properties, String tagDefinitionUuid) {
		for (Property property : properties) {
			// add entries to the context that allow us to have PropertyDefinition URLs aliased by name
			this.context.put(
					property.getName(),
					String.format(
						"http://catma.de/portal/tag/%s/property/%s",  // TODO: actual GitLab URL
						tagDefinitionUuid,
						property.getPropertyDefinition().getUuid()
					)
			);

			// add property values
			this.properties.put(property.getName(), new TreeSet<>(property.getPropertyValueList().getValues()));
		}
	}

	@JsonProperty(to="@context")
	public TreeMap<String, String> getContext() {
		return this.context;
	}

	@JsonProperty(from="@context")
	public void setContext(TreeMap<String, String> context) {
		this.context = context;
	}

	public String getType() {
		return "Dataset";
	}

	public String getTagset() {
		return this.tagset;
	}

	public void setTagset(String tagset) {
		this.tagset = tagset;
	}

	public String getTag() {
		return this.tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public TreeMap<String, TreeSet<String>> getProperties() {
		return this.properties;
	}

	public void setProperties(TreeMap<String, TreeSet<String>> properties) {
		this.properties = properties;
	}
}
