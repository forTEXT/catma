package de.catma.repository.git.serialization.models.json_ld;

import com.jsoniter.annotation.JsonProperty;
import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.repository.git.exceptions.JsonLdWebAnnotationException;
import de.catma.tag.TagInstance;

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

	public JsonLdWebAnnotation(List<TagReference> tagReferences) throws JsonLdWebAnnotationException {
		// assert that all TagReference objects are for the same TagInstance and thus share the same TagDefinition and
		// properties
		Set<TagInstance> uniqueTagInstances = new HashSet<>(tagReferences.stream().map(TagReference::getTagInstance)
				.collect(Collectors.toSet()));
		if (uniqueTagInstances.size() > 1) {
			throw new JsonLdWebAnnotationException(
				"Supplied TagReference objects are not all for the same TagInstance"
			);
		}

		this.id = String.format(
			"http://catma.de/portal/annotation/%s", tagReferences.get(0).getTagInstance().getUuid()
		);  // TODO: actual GitLab URL

		this.body = new JsonLdWebAnnotationBody_Dataset(tagReferences);
		this.target = new JsonLdWebAnnotationTarget_List(tagReferences);
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
		throw new JsonLdWebAnnotationException("Not implemented");
	}
}
