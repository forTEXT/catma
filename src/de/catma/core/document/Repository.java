package de.catma.core.document;

import java.util.Set;

import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.structure.StructureMarkupCollection;
import de.catma.core.document.standoffmarkup.user.UserMarkupCollection;

public interface Repository {

	public Set<SourceDocument> getSourceDocuments();
	public Set<Collection> getCollections();
	
	public void delete(SourceDocument sourceDocument);
	public void delete(UserMarkupCollection userMarkupCollection);
	public void delete(StructureMarkupCollection structureMarkupCollection);
	
	public void update(SourceDocument sourceDocument);
	public void update(UserMarkupCollection userMarkupCollection);
	public void update(StructureMarkupCollection structureMarkupCollection);

	public void insert(SourceDocument sourceDocument);
	public void insert(UserMarkupCollection userMarkupCollection);
	public void insert(StructureMarkupCollection structureMarkupCollection);
	
}
