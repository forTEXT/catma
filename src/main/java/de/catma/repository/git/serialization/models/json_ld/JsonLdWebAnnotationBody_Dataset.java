package de.catma.repository.git.serialization.models.json_ld;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.gson.annotations.SerializedName;

import de.catma.document.annotation.TagReference;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.GitProjectHandler;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;

/**
 * Represents a Web Annotation Data Model conformant annotation body of type 'Dataset'.
 *
 * @see <a href="https://www.w3.org/TR/annotation-model/#classes">Web Annotation Data Model - Bodies and Targets - Classes</a>
 */
public class JsonLdWebAnnotationBody_Dataset {
	// we use the TreeMap and TreeSet types so that we get automatic sorting
	private @SerializedName("@context") TreeMap<String, String> context;
	private String tagset; // tagset URI, do not rename
	private String tag; // tag URI, do not rename
	private TreeMap<String, TreeMap<String, TreeSet<String>>> properties;
	@SuppressWarnings("unused") // used by reflection
	private String type = "Dataset";

	public final static String SYSTEM_PROPERTIES_KEY = "system";
	public final static String USER_PROPERTIES_KEY = "user";

	public JsonLdWebAnnotationBody_Dataset() {
		this.context = new TreeMap<>();
		this.properties = new TreeMap<>();
		this.properties.put(SYSTEM_PROPERTIES_KEY, new TreeMap<>());
		this.properties.put(USER_PROPERTIES_KEY, new TreeMap<>());
	}

	public JsonLdWebAnnotationBody_Dataset(Collection<TagReference> tagReferences, TagLibrary tagLibrary) throws IOException {
		this();

		String contextDefinitionURL = CATMAPropertyKey.CONTEXT_DEFINITION_URL.getValue();
		if (!contextDefinitionURL.endsWith("/")) {
			contextDefinitionURL += "/";
		}
		this.context.put("tagset", contextDefinitionURL + "tagset");
		this.context.put("tag", contextDefinitionURL + "tag");

		// assert that all TagReference objects are for the same TagInstance and thus share the same TagDefinition and properties
		Set<TagInstance> uniqueTagInstances = new HashSet<>(
				tagReferences.stream().map(TagReference::getTagInstance).collect(Collectors.toSet())
		);
		if (uniqueTagInstances.size() > 1) {
			throw new IllegalArgumentException("Supplied TagReference objects are not all for the same TagInstance");
		}

		String tagDefinitionId = tagReferences.iterator().next().getTagDefinitionId();
		TagDefinition tagDefinition = tagLibrary.getTagDefinition(tagDefinitionId);

		TagInstance tagInstance = tagReferences.iterator().next().getTagInstance();

		this.tagset = this.buildTagsetUri(tagInstance.getTagsetId()).toString();
		this.tag = this.buildTagDefinitionUri(tagDefinition).toString();

		this.addProperties(tagInstance.getUserDefinedProperties(), false);
		this.addProperties(tagInstance.getSystemProperties(), true);
	}

	private URI buildTagsetUri(String tagsetUuid) throws IOException {
		try {
			return new URI(
				String.format(
						"%s/%s",
						GitProjectHandler.TAGSETS_DIRECTORY_NAME,
						tagsetUuid
				)
			);
		}
		catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	private URI buildTagDefinitionUri(TagDefinition tagDefinition) throws IOException{
		try {
			return new URI(
				String.format("%s/%s", this.tagset, tagDefinition.getUuid())
			);
		}
		catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	private URI buildPropertyDefinitionUri(String propertyDefinitionUuid) throws IOException {
		try {
			return new URI(
				String.format("%s/%s", this.tag, propertyDefinitionUuid)
			);
		}
		catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	private void addProperties(Collection<Property> properties, boolean system) throws IOException {
		for (Property property : properties) {
			// add entries to the context that allow us to have PropertyDefinition URLs aliased by name
			this.context.put(
				property.getPropertyDefinitionId(),
				this.buildPropertyDefinitionUri(property.getPropertyDefinitionId()).toString()
			);

			// add property values
			if (system) {
				this.properties.get("system").put(
					property.getPropertyDefinitionId(), new TreeSet<>(property.getPropertyValueList())
				);
			}
			else {
				this.properties.get("user").put(
					property.getPropertyDefinitionId(), new TreeSet<>(property.getPropertyValueList())
				);
			}
		}
	}

	public String getTagset() {
		return this.tagset;
	}

	public String getTag() {
		return this.tag;
	}

	public TreeMap<String, TreeMap<String, TreeSet<String>>> getProperties() {
		return this.properties;
	}
}
