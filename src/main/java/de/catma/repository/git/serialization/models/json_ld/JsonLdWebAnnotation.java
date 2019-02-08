package de.catma.repository.git.serialization.models.json_ld;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.jsoniter.annotation.JsonIgnore;
import com.jsoniter.annotation.JsonProperty;

import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.git.GitProjectHandler;
import de.catma.repository.git.GitProjectManager;
import de.catma.repository.git.GitTagsetHandler;
import de.catma.repository.git.interfaces.ILocalGitRepositoryManager;
import de.catma.repository.git.interfaces.IRemoteGitServerManager;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.tag.Version;

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

	public JsonLdWebAnnotation(String gitServerBaseUrl, String projectId, List<TagReference> tagReferences, TagLibrary tagLibrary)
			throws IOException {
		// assert that all TagReference objects are for the same TagInstance and thus share the same TagDefinition and
		// properties
		Set<TagInstance> uniqueTagInstances = new HashSet<>(tagReferences.stream().map(TagReference::getTagInstance)
				.collect(Collectors.toSet()));
		if (uniqueTagInstances.size() > 1) {
			throw new IOException(
				"Supplied TagReference objects are not all for the same TagInstance"
			);
		}

		String projectRootRepositoryName = GitProjectManager.getProjectRootRepositoryName(projectId);

		this.id = this.buildTagInstanceUrl(
			gitServerBaseUrl, projectRootRepositoryName, tagReferences.get(0).getUserMarkupCollectionUuid(),
			tagReferences.get(0).getTagInstance().getUuid()
		).toString();

		this.body = new JsonLdWebAnnotationBody_Dataset(gitServerBaseUrl, projectId, tagReferences, tagLibrary);
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
				gitServerUrl.getProtocol(),
				gitServerUrl.getHost(),
				gitServerUrl.getPort(),
				String.format(
						"%s%s/%s/%s/annotations/%s.json",
						gitServerUrl.getPath(),
						projectRootRepositoryName,
						GitProjectHandler.MARKUP_COLLECTION_SUBMODULES_DIRECTORY_NAME,
						userMarkupCollectionUuid,
						tagInstanceUuid
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
				throws IOException {
		TagInstance tagInstance = this.getTagInstance(localGitRepositoryManager, remoteGitServerManager, projectId);
		String sourceDocumentUri = this.getSourceDocumentUri();
		List<Range> ranges = this.getRanges();

		// TODO: figure out how to do this with .stream().map while handling exceptions properly
		// see https://stackoverflow.com/a/33218789 & https://stackoverflow.com/a/30118121 for pointers
		List<TagReference> tagReferences = new ArrayList<>();
		try {
			for (Range range : ranges) {
				tagReferences.add(new TagReference(tagInstance, sourceDocumentUri, range, markupCollectionId));
			}
		}
		catch (URISyntaxException e) {
			throw new IOException(
				"Failed to turn internal representation back into a collection of TagReference objects", e
			);
		}

		return tagReferences;
	}

	private String getSourceDocumentUri() throws IOException {
		return this.target.getItems().first().getSource();
	}

	private List<Range> getRanges() throws IOException {
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
			throws IOException {
		TagDefinition tagDefinition = this.getTagDefinition(
			localGitRepositoryManager, remoteGitServerManager, projectId
		);

		TagInstance tagInstance = new TagInstance(
			this.getTagInstanceUuid(),
			tagDefinition.getUuid(),
			tagDefinition.getAuthor(),
			ZonedDateTime.now().format(DateTimeFormatter.ofPattern(Version.DATETIMEPATTERN)),
			tagDefinition.getUserDefinedPropertyDefinitions(),
			tagDefinition.getTagsetDefinitionUuid()
		);

		TreeMap<String, TreeMap<String, TreeSet<String>>> properties = this.body.getProperties();

		for (Map.Entry<String, TreeMap<String, TreeSet<String>>> entry : properties.entrySet()) {
			for (Map.Entry<String, TreeSet<String>> subEntry : entry.getValue().entrySet()) {
				Property property = new Property(
					subEntry.getKey(),
					subEntry.getValue()
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
			throws IOException {
		GitTagsetHandler gitTagsetHandler = new GitTagsetHandler(localGitRepositoryManager, remoteGitServerManager);

		// TODO: open a TagDefinition directly?
		TagsetDefinition tagsetDefinition = gitTagsetHandler.open(
				projectId,
				this.getLastPathSegmentFromUrl(this.body.getTagset())
		);
		return tagsetDefinition.getTagDefinition(this.getLastPathSegmentFromUrl(this.body.getTag()));

	}

	private String getLastPathSegmentFromUrl(String url) {
		return url.substring(url.lastIndexOf("/") + 1);
	}
}
