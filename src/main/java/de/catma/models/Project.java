package de.catma.models;

import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.interfaces.ISourceControlVersionable;
import de.catma.tag.TagsetDefinition;

import java.util.ArrayList;
import java.util.List;

public class Project implements ISourceControlVersionable {
	private String uuid;
	private String name;
	private String description;
	private String revisionHash;

	private List<TagsetDefinition> tagsets;
	private List<UserMarkupCollection> markupCollections;
	private List<SourceDocument> sourceDocuments;

	public Project() {
		this.tagsets = new ArrayList<>();
		this.markupCollections = new ArrayList<>();
		this.sourceDocuments = new ArrayList<>();
	}

	public Project(String uuid, String name, String description, String revisionHash) {
		this();

		this.uuid = uuid;
		this.name = name;
		this.description = description;
		this.revisionHash = revisionHash;
	}

	public String getUuid() {
		return this.uuid;
	}

	public String getName() {
		return this.name;
	}

	public String getDescription() {
		return this.description;
	}

	public List<TagsetDefinition> getTagsets() {
		return this.tagsets;
	}

	public List<UserMarkupCollection> getMarkupCollections() {
		return this.markupCollections;
	}

	public List<SourceDocument> getSourceDocuments() {
		return this.sourceDocuments;
	}

	public void addTagset(TagsetDefinition tagsetDefinition) {
		// TODO: validation
		this.tagsets.add(tagsetDefinition);
	}

	public void addMarkupCollection(UserMarkupCollection userMarkupCollection) {
		// TODO: validation
		this.markupCollections.add(userMarkupCollection);
	}

	public void addSourceDocument(SourceDocument sourceDocument) {
		// TODO: validation
		this.sourceDocuments.add(sourceDocument);
	}

	@Override
	public String getRevisionHash() {
		return this.revisionHash;
	}

	@Override
	public void setRevisionHash(String revisionHash) {
		this.revisionHash = revisionHash;
	}
}
