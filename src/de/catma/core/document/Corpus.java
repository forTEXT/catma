package de.catma.core.document;

import java.util.ArrayList;
import java.util.List;

import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.structure.StructureMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.user.UserMarkupCollectionReference;

public class Corpus {

	private String name;

	private List<SourceDocument> sourceDocuments;
	private List<StructureMarkupCollectionReference> structureMarkupCollectionRefs;
	private List<UserMarkupCollectionReference> userMarkupCollectionRefs;
	
	public Corpus(String corpusName) {
		this.name = corpusName;
		this.sourceDocuments = new ArrayList<SourceDocument>();
		this.structureMarkupCollectionRefs = new ArrayList<StructureMarkupCollectionReference>();
		this.userMarkupCollectionRefs = new ArrayList<UserMarkupCollectionReference>();
	}

	public void addSourceDocument(SourceDocument sourceDocument) {
		sourceDocuments.add(sourceDocument);
	}

	public void addStructureMarkupCollectionReference(
			StructureMarkupCollectionReference structureMarkupCollRef) {
		structureMarkupCollectionRefs.add(structureMarkupCollRef);
	}

	public void addUserMarkupCollectionReference(
			UserMarkupCollectionReference userMarkupCollRef) {
		userMarkupCollectionRefs.add(userMarkupCollRef);
	}
	
}
