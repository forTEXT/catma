package de.catma.repository.git.serialization.models.json_ld;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.jsoniter.annotation.JsonProperty;

import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.GitProjectManager;
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

	public JsonLdWebAnnotationBody_Dataset(
		String gitServerBaseUrl, String projectId, List<TagReference> tagReferences, TagLibrary tagLibrary)
			throws IOException {
		this();
		this.context.put("tagset", "http://catma.de/portal/tagset");  // TODO: what should this URL be?
		this.context.put("tag", "http://catma.de/portal/tag");  // TODO: what should this URL be?

		// assert that all TagReference objects are for the same TagInstance and thus share the same TagDefinition and
		// properties
		Set<TagInstance> uniqueTagInstances = new HashSet<>(tagReferences.stream().map(TagReference::getTagInstance)
				.collect(Collectors.toSet()));
		if (uniqueTagInstances.size() > 1) {
			throw new IllegalArgumentException(
				"Supplied TagReference objects are not all for the same TagInstance"
			);
		}

		String tagDefinitionId = tagReferences.get(0).getTagDefinitionId();
		TagDefinition tagDefinition = tagLibrary.getTagDefinition(tagDefinitionId);
		
		TagInstance tagInstance = tagReferences.get(0).getTagInstance();

		String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

		this.tagset = this.buildTagsetUrl(
			gitServerBaseUrl, projectRootRepositoryName, tagInstance.getTagsetId()
		).toString();

		this.tag = this.buildTagDefinitionUrl(this.tagset, tagDefinition).toString();

		this.addProperties(this.tag, tagInstance.getUserDefinedProperties(), false);
		this.addProperties(this.tag, tagInstance.getSystemProperties(), true);
	}

	private URL buildTagsetUrl(String gitServerBaseUrl, String projectRootRepositoryName, String tagsetUuid)
			throws MalformedURLException {
		URL gitServerUrl = JsonLdWebAnnotation.sanitizeUrl(gitServerBaseUrl);

		return new URL(
				gitServerUrl.getProtocol(),
				gitServerUrl.getHost(),
				gitServerUrl.getPort(),
				String.format(
						"%s%s/%s/%s",
						gitServerUrl.getPath(),
						projectRootRepositoryName,
						GitProjectHandler.TAGSET_SUBMODULES_DIRECTORY_NAME,
						tagsetUuid
				)
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
				property.getPropertyDefinitionId(),
				this.buildPropertyDefinitionUrl(tagDefinitionUrl, property.getPropertyDefinitionId()).toString()
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
