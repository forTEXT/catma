package de.catma.repository.git.serialization.models.json_ld;

import com.jsoniter.annotation.JsonIgnore;
import com.jsoniter.annotation.JsonProperty;
import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.git.ProjectHandler;
import de.catma.repository.git.TagsetHandler;
import de.catma.repository.git.exceptions.JsonLdWebAnnotationException;
import de.catma.repository.git.exceptions.TagsetHandlerException;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.tag.*;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents an annotation instance that, when serialized, conforms to the Web Annotation Data Model and JSON-LD
 * specifications.
 *
 * @see <a href="https://www.w3.org/TR/annotation-model/">Web Annotation Data Model</a>
 * @see <a href="https://json-ld.org/spec/latest/json-ld/">JSON-LD</a>
 */
public class JsonLdWebAnnotation {
	private String id;
	private JsonLdWebAnnotationBody_Dataset body;
	private JsonLdWebAnnotationTarget_List target;

	public JsonLdWebAnnotation() {

	}

	public JsonLdWebAnnotation(String gitServerBaseUrl, String projectId, List<TagReference> tagReferences)
			throws JsonLdWebAnnotationException {
		// assert that all TagReference objects are for the same TagInstance and thus share the same TagDefinition and
		// properties
		Set<TagInstance> uniqueTagInstances = new HashSet<>(tagReferences.stream().map(TagReference::getTagInstance)
				.collect(Collectors.toSet()));
		if (uniqueTagInstances.size() > 1) {
			throw new JsonLdWebAnnotationException(
				"Supplied TagReference objects are not all for the same TagInstance"
			);
		}

		String projectRootRepositoryName = ProjectHandler.getProjectRootRepositoryName(projectId);

		try {
			this.id = this.buildTagInstanceUrl(
				gitServerBaseUrl, projectRootRepositoryName, tagReferences.get(0).getUserMarkupCollectionUuid(),
				tagReferences.get(0).getTagInstance().getUuid()
			).toString();
		}
		catch (MalformedURLException e) {
			throw new JsonLdWebAnnotationException("Failed to build tag instance URL", e);
		}

		this.body = new JsonLdWebAnnotationBody_Dataset(gitServerBaseUrl, projectId, tagReferences);
		this.target = new JsonLdWebAnnotationTarget_List(tagReferences);
	}

	static URL sanitizeUrl(String url) throws MalformedURLException {
		// absence of a trailing slash on the file/path component is handled really badly by both URI and URL
		// URL(URL context, String spec) only works if the file/path component of context has a trailing slash...
		// URI normalize or resolve methods do not fix it either
		// NB: this method does not care about query params and will strip them if they exist in the URL
		URL _url = new URL(url);
		String path = _url.getPath();
		if (!path.endsWith("/")) {
			path = path + "/";
		}
		return new URL(_url.getProtocol(), _url.getHost(), _url.getPort(), path);
	}

	private URL buildTagInstanceUrl(String gitServerBaseUrl, String projectRootRepositoryName,
									String userMarkupCollectionUuid, String tagInstanceUuid)
			throws MalformedURLException {

		URL gitServerUrl = JsonLdWebAnnotation.sanitizeUrl(gitServerBaseUrl);

		return new URL(
			gitServerUrl.getProtocol(), gitServerUrl.getHost(), gitServerUrl.getPort(),
			String.format("%s%s/collections/%s/annotations/%s.json",
				gitServerUrl.getPath(), projectRootRepositoryName, userMarkupCollectionUuid, tagInstanceUuid
			)
		);
	}

	@JsonProperty(to="@context")
	public String getContext() {
		return "http://www.w3.org/ns/anno.jsonld";
	}

	public String getType() {
		return "Annotation";
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public JsonLdWebAnnotationBody_Dataset getBody() {
		return this.body;
	}

	public void setBody(JsonLdWebAnnotationBody_Dataset body) {
		this.body = body;
	}

	public JsonLdWebAnnotationTarget_List getTarget() {
		return this.target;
	}

	public void setTarget(JsonLdWebAnnotationTarget_List target) {
		this.target = target;
	}

	public List<TagReference> toTagReferenceList(
			String projectId, String markupCollectionId,
			ILocalGitRepositoryManager localGitRepositoryManager, IRemoteGitServerManager remoteGitServerManager)
				throws JsonLdWebAnnotationException {
		TagInstance tagInstance = this.getTagInstance(localGitRepositoryManager, remoteGitServerManager, projectId);
		String sourceDocumentUri = this.getSourceDocumentUri();
		List<Range> ranges = this.getRanges();

		// TODO: figure out how to do this with .stream().map while handling exceptions properly
		// see https://stackoverflow.com/a/33218789 & https://stackoverflow.com/a/30118121 for pointers
		ArrayList<TagReference> tagReferences = new ArrayList<>();
		try {
			for (Range range : ranges) {
				tagReferences.add(new TagReference(tagInstance, sourceDocumentUri, range, markupCollectionId));
			}
		}
		catch (URISyntaxException e) {
			throw new JsonLdWebAnnotationException(
				"Failed to turn internal representation back into a collection of TagReference objects", e
			);
		}

		return tagReferences;
	}

	private String getSourceDocumentUri() throws JsonLdWebAnnotationException {
		return this.target.getItems().first().getSource();
	}

	private List<Range> getRanges() throws JsonLdWebAnnotationException {
		return this.target.getItems().stream().map(jsonLdWebAnnotationTarget -> {
			JsonLdWebAnnotationTextPositionSelector selector = jsonLdWebAnnotationTarget.getSelector();
			return new Range(selector.getStart(), selector.getEnd());
		}).collect(Collectors.toList());
	}

	@JsonIgnore
	public String getTagInstanceUuid() {
		return this.getLastPathSegmentFromUrl(this.id).replace(".json", "");
	}

	@JsonIgnore
	public TagInstance getTagInstance(ILocalGitRepositoryManager localGitRepositoryManager,
									  IRemoteGitServerManager remoteGitServerManager, String projectId)
			throws JsonLdWebAnnotationException {
		TagDefinition tagDefinition = this.getTagDefinition(
			localGitRepositoryManager, remoteGitServerManager, projectId
		);

		TagInstance tagInstance = new TagInstance(
			this.getTagInstanceUuid(),
			tagDefinition
		);

		// the TagInstance constructor sets default values for system properties, so we need to clear them
		for (Property property : tagInstance.getSystemProperties()) {
			property.setPropertyValueList(new PropertyValueList());
		}

		TreeMap<String, TreeMap<String, TreeSet<String>>> properties = this.body.getProperties();

		for (Map.Entry<String, TreeMap<String, TreeSet<String>>> entry : properties.entrySet()) {
			for (Map.Entry<String, TreeSet<String>> subEntry : entry.getValue().entrySet()) {
				Property property = new Property(
					tagDefinition.getPropertyDefinitionByName(subEntry.getKey()),
					new PropertyValueList(new ArrayList<>(subEntry.getValue()))
				);
				if (entry.getKey().equals(JsonLdWebAnnotationBody_Dataset.SYSTEM_PROPERTIES_KEY)) {
					tagInstance.addSystemProperty(property);
				}
				else {
					tagInstance.addUserDefinedProperty(property);
				}
			}
		}

		return tagInstance;
	}

	private TagDefinition getTagDefinition(ILocalGitRepositoryManager localGitRepositoryManager,
										   IRemoteGitServerManager remoteGitServerManager, String projectId)
			throws JsonLdWebAnnotationException {
		TagsetHandler tagsetHandler = new TagsetHandler(localGitRepositoryManager, remoteGitServerManager);

		try {
			// TODO: open a TagDefinition directly?
			TagsetDefinition tagsetDefinition = tagsetHandler.open(
					projectId,
					this.getLastPathSegmentFromUrl(this.body.getTagset())
			);
			return tagsetDefinition.getTagDefinition(this.getLastPathSegmentFromUrl(this.body.getTag()));
		}
		catch (TagsetHandlerException e) {
			throw new JsonLdWebAnnotationException("Failed to open tagset", e);
		}
	}

	private String getLastPathSegmentFromUrl(String url) {
		return url.substring(url.lastIndexOf("/") + 1);
	}
}
