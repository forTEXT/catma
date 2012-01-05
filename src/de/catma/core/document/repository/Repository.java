package de.catma.core.document.repository;

import java.util.Collection;
import java.util.Set;

import de.catma.core.document.Corpus;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.structure.StructureMarkupCollection;
import de.catma.core.document.standoffmarkup.structure.StructureMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.user.UserMarkupCollection;
import de.catma.core.document.standoffmarkup.user.UserMarkupCollectionReference;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagLibraryReference;

public interface Repository {
	
	public String getName();
	public void open() throws Exception;

	public Collection<SourceDocument> getSourceDocuments();
	public Set<Corpus> getCorpora();
	public Set<TagLibraryReference> getTagLibraryReferences();

	public UserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference);
	
	public StructureMarkupCollection getStructureMarkupCollection(
			StructureMarkupCollectionReference structureMarkupCollectionReference);
	
	public TagLibrary getTagLibrary(TagLibraryReference tagLibraryReference);
	
	public void delete(SourceDocument sourceDocument);
	public void delete(UserMarkupCollection userMarkupCollection);
	public void delete(StructureMarkupCollection structureMarkupCollection);
	
	public void update(SourceDocument sourceDocument);
	public void update(UserMarkupCollection userMarkupCollection);
	public void update(StructureMarkupCollection structureMarkupCollection);

	public void insert(SourceDocument sourceDocument);
	public UserMarkupCollectionReference insert(
			UserMarkupCollection userMarkupCollection);
	public StructureMarkupCollectionReference insert(
			StructureMarkupCollection structureMarkupCollection);
	
}
