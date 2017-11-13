package de.catma.repository.git.serialization.models.json_ld;

import com.jsoniter.annotation.JsonProperty;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.git.ProjectHandler;
import de.catma.repository.git.exceptions.JsonLdWebAnnotationException;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
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
	private TreeMap<String, TreeMap<String, TreeSet<String>>> properties;

	public final static String SYSTEM_PROPERTIES_KEY = "system";
	public final static String USER_PROPERTIES_KEY = "user";

	public JsonLdWebAnnotationBody_Dataset() {
		this.context = new TreeMap<>();
		this.properties = new TreeMap<>();
		this.properties.put(SYSTEM_PROPERTIES_KEY, new TreeMap<>());
		this.properties.put(USER_PROPERTIES_KEY, new TreeMap<>());
	}

	public JsonLdWebAnnotationBody_Dataset(String gitServerBaseUrl, String projectId, List<TagReference> tagReferences)
			throws JsonLdWebAnnotationException {
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

		TagDefinition tagDefinition = tagReferences.get(0).getTagDefinition();
		TagInstance tagInstance = tagReferences.get(0).getTagInstance();

		String projectRootRepositoryName = ProjectHandler.getProjectRootRepositoryName(projectId);

		try {
			this.tagset = this.buildTagsetUrl(
				gitServerBaseUrl, projectRootRepositoryName, tagDefinition.getTagsetDefinitionUuid()
			).toString();

			this.tag = this.buildTagDefinitionUrl(this.tagset, tagDefinition).toString();

			this.addProperties(this.tag, tagInstance.getUserDefinedProperties(), false);
			this.addProperties(this.tag, tagInstance.getSystemProperties(), true);
		}
		catch (MalformedURLException e) {
			throw new JsonLdWebAnnotationException("Failed to build tagset URL", e);
		}
	}

	private URL buildTagsetUrl(String gitServerBaseUrl, String projectRootRepositoryName, String tagsetUuid)
			throws MalformedURLException {
		URL gitServerUrl = JsonLdWebAnnotation.sanitizeUrl(gitServerBaseUrl);

		return new URL(
			gitServerUrl.getProtocol(), gitServerUrl.getHost(), gitServerUrl.getPort(),
			String.format("%s%s/tagsets/%s", gitServerUrl.getPath(), projectRootRepositoryName, tagsetUuid)
		);
	}

	private URL buildTagDefinitionUrl(String tagsetUrl, TagDefinition tagDefinition)
			throws MalformedURLException {
		URL _tagsetUrl = JsonLdWebAnnotation.sanitizeUrl(tagsetUrl);

		return new URL(
			_tagsetUrl.getProtocol(), _tagsetUrl.getHost(), _tagsetUrl.getPort(),
			StringUtils.isEmpty(tagDefinition.getParentUuid()) ?
					String.format("%s%s", _tagsetUrl.getPath(), tagDefinition.getUuid()) :
					String.format(
						"%s%s/%s", _tagsetUrl.getPath(), tagDefinition.getParentUuid(), tagDefinition.getUuid()
					)
		);
	}

	private URL buildPropertyDefinitionUrl(String tagDefinitionUrl, String propertyDefinitionUuid)
			throws MalformedURLException {
		URL _tagDefinitionUrl = JsonLdWebAnnotation.sanitizeUrl(tagDefinitionUrl);

		return new URL(
			_tagDefinitionUrl.getProtocol(), _tagDefinitionUrl.getHost(), _tagDefinitionUrl.getPort(),
			String.format("%spropertydefs.json/%s", _tagDefinitionUrl.getPath(), propertyDefinitionUuid)
		);
	}

	private void addProperties(String tagDefinitionUrl, Collection<Property> properties, boolean system)
			throws MalformedURLException {
		for (Property property : properties) {
			// add entries to the context that allow us to have PropertyDefinition URLs aliased by name
			this.context.put(
				property.getName(),
				this.buildPropertyDefinitionUrl(tagDefinitionUrl, property.getPropertyDefinition().getUuid()).toString()
			);

			// add property values
			if (system) {
				this.properties.get("system").put(
					property.getName(), new TreeSet<>(property.getPropertyValueList().getValues())
				);
			}
			else {
				this.properties.get("user").put(
					property.getName(), new TreeSet<>(property.getPropertyValueList().getValues())
				);
			}
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

	public TreeMap<String, TreeMap<String, TreeSet<String>>> getProperties() {
		return this.properties;
	}

	public void setProperties(TreeMap<String, TreeMap<String, TreeSet<String>>> properties) {
		this.properties = properties;
	}
}
