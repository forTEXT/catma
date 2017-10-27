package de.catma.repository.git.serialization.models.json_ld;

import com.jsoniter.annotation.JsonIgnore;
import com.jsoniter.annotation.JsonProperty;
import de.catma.document.Range;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.git.ProjectHandler;
import de.catma.repository.git.exceptions.JsonLdWebAnnotationException;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

		String projectRootRepositoryName = ProjectHandler.getProjectRepoName(projectId);

		try {
			this.id = this.buildTagInstanceUrl(
				gitServerBaseUrl, projectRootRepositoryName, tagReferences.get(0).getTagInstance().getUuid()
			).toString();
		}
		catch (MalformedURLException e) {
			throw new JsonLdWebAnnotationException("Failed to build tag instance URL", e);
		}
		
//		this.id = String.format(
//			"http://catma.de/portal/annotation/%s", tagReferences.get(0).getTagInstance().getUuid()
//		);  // TODO: actual GitLab URL

		this.body = new JsonLdWebAnnotationBody_Dataset(tagReferences);
		this.target = new JsonLdWebAnnotationTarget_List(tagReferences);
	}

	private URL buildTagInstanceUrl(String gitServerBaseUrl, String projectRootRepositoryName, String tagInstanceUuid)
			throws MalformedURLException {
		URL gitServerUrl = new URL(gitServerBaseUrl);
		return new URL(
			gitServerUrl.getProtocol(), gitServerUrl.getHost(), gitServerUrl.getPort(),
			String.format("%s/%s", projectRootRepositoryName, tagInstanceUuid)
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

	public List<TagReference> toTagReferenceList() throws JsonLdWebAnnotationException {
		TagInstance tagInstance = this.getTagInstance();
		String sourceDocumentUri = this.getSourceDocumentUri();
		List<Range> ranges = this.getRanges();

		// TODO: figure out how to do this with .stream().map while handling exceptions properly
		// see https://stackoverflow.com/a/33218789 & https://stackoverflow.com/a/30118121 for pointers
		ArrayList<TagReference> tagReferences = new ArrayList<>();
		try {
			for (Range range : ranges) {
				tagReferences.add(new TagReference(tagInstance, sourceDocumentUri, range));
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
	public TagInstance getTagInstance() throws JsonLdWebAnnotationException {
//		TagDefinition tagDefinition = this.getTagDefinition();
		throw new JsonLdWebAnnotationException("Not implemented");
	}

	private TagDefinition getTagDefinition() throws JsonLdWebAnnotationException {
		throw new JsonLdWebAnnotationException("Not implemented");
	}
}
