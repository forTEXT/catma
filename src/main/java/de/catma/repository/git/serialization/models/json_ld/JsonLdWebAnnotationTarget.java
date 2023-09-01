package de.catma.repository.git.serialization.models.json_ld;

import de.catma.document.Range;
import de.catma.document.annotation.TagReference;

import java.util.Objects;

/**
 * Represents a Web Annotation Data Model conformant annotation target with a selector class of 'TextPositionSelector'.
 */
public class JsonLdWebAnnotationTarget implements Comparable<JsonLdWebAnnotationTarget> {
	private String source;
	private JsonLdWebAnnotationTextPositionSelector selector;

	public JsonLdWebAnnotationTarget() {

	}

	public JsonLdWebAnnotationTarget(TagReference tagReference) {
		this.source = tagReference.getSourceDocumentId().toString();

		Range range = tagReference.getRange();
		this.selector = new JsonLdWebAnnotationTextPositionSelector(range.getStartPoint(), range.getEndPoint());
	}

	public String getSource() {
		return this.source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public JsonLdWebAnnotationTextPositionSelector getSelector() {
		return this.selector;
	}

	public void setSelector(JsonLdWebAnnotationTextPositionSelector selector) {
		this.selector = selector;
	}

	@Override
	public int compareTo(JsonLdWebAnnotationTarget o) {
		if (!this.source.equals(o.getSource())) {
			return this.source.compareTo(o.getSource());
		}
		return this.selector.compareTo(o.getSelector());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}

		return this.compareTo((JsonLdWebAnnotationTarget)obj) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.source, this.selector);
	}
}
