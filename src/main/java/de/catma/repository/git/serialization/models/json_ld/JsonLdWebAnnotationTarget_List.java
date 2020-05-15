package de.catma.repository.git.serialization.models.json_ld;

import java.util.Collection;
import java.util.TreeSet;
import java.util.stream.Collectors;

import de.catma.document.annotation.TagReference;

/**
 * Represents a Web Annotation Data Model conformant annotation target of type 'List'.
 *
 * @see <a href="https://www.w3.org/TR/annotation-model/#sets-of-bodies-and-targets">Web Annotation Data Model - Sets of Bodies and Targets</a>
 */
public class JsonLdWebAnnotationTarget_List {
	// we use the TreeSet type so that we get automatic sorting (JsonLdWebAnnotationTarget implements the Comparable
	// interface)
	private TreeSet<JsonLdWebAnnotationTarget> items;
	private String type = "List";

	public JsonLdWebAnnotationTarget_List() {
		this.items = new TreeSet<>();
	}

	public JsonLdWebAnnotationTarget_List(Collection<TagReference> tagReferences) {
		this();
		this.items = new TreeSet<>(tagReferences.stream().map(JsonLdWebAnnotationTarget::new)
				.collect(Collectors.toList()));
	}

	public String getType() {
		return this.type;
	}

	public TreeSet<JsonLdWebAnnotationTarget> getItems() {
		return this.items;
	}

	public void setItems(TreeSet<JsonLdWebAnnotationTarget> items) {
		this.items = items;
	}
}
