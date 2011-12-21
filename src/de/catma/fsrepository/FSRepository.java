package de.catma.fsrepository;

import java.util.Set;

import de.catma.core.document.Collection;
import de.catma.core.document.Repository;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.structure.StructureMarkupCollection;
import de.catma.core.document.standoffmarkup.structure.StructureMarkupCollectionReference;
import de.catma.core.document.standoffmarkup.user.UserMarkupCollection;
import de.catma.core.document.standoffmarkup.user.UserMarkupCollectionReference;

public class FSRepository implements Repository {
	
	private String repoFolderPath;
	
	public FSRepository(String repoFolderPath) {
		super();
		this.repoFolderPath = repoFolderPath;
		loadCollections();
	}

	private void loadCollections() {
		// TODO Auto-generated method stub
		
	}

	public Set<SourceDocument> getSourceDocuments() {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<Collection> getCollections() {
		
		return null;
	}

	public UserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference) {
		// TODO Auto-generated method stub
		return null;
	}

	public StructureMarkupCollection getStructureMarkupCollection(
			StructureMarkupCollectionReference structureMarkupCollectionReference) {
		// TODO Auto-generated method stub
		return null;
	}

	public void delete(SourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		
	}

	public void delete(UserMarkupCollection userMarkupCollection) {
		// TODO Auto-generated method stub
		
	}

	public void delete(StructureMarkupCollection structureMarkupCollection) {
		// TODO Auto-generated method stub
		
	}

	public void update(SourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		
	}

	public void update(UserMarkupCollection userMarkupCollection) {
		// TODO Auto-generated method stub
		
	}

	public void update(StructureMarkupCollection structureMarkupCollection) {
		// TODO Auto-generated method stub
		
	}

	public void insert(SourceDocument sourceDocument) {
		// TODO Auto-generated method stub
		
	}

	public UserMarkupCollectionReference insert(
			UserMarkupCollection userMarkupCollection) {
		// TODO Auto-generated method stub
		return null;
	}

	public StructureMarkupCollectionReference insert(
			StructureMarkupCollection structureMarkupCollection) {
		// TODO Auto-generated method stub
		return null;
	}

	

}
