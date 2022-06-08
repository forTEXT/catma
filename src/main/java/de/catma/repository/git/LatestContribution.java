package de.catma.repository.git;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LatestContribution {
	
	private final String branch;
	private final Set<String> collectionIds;
	private final Set<String> documentIds;
	private final Set<String> tagsetIds;
	public LatestContribution(String branch) {
		super();
		this.branch = branch;
		this.collectionIds = new HashSet<String>();
		this.documentIds = new HashSet<String>();
		this.tagsetIds = new HashSet<String>();
	}
	
	public String getBranch() {
		return branch;
	}
	public Set<String> getCollectionIds() {
		return Collections.unmodifiableSet(collectionIds);
	}
	public Set<String> getDocumentIds() {
		return Collections.unmodifiableSet(documentIds);
	}
	public Set<String> getTagsetIds() {
		return Collections.unmodifiableSet(tagsetIds);
	}
	
	public void addCollectionId(String collectionId) {
		this.collectionIds.add(collectionId);
	}
	
	public void addDocumentId(String documentId) {
		this.documentIds.add(documentId);
	}
	
	public void addTagsetId(String tagsetId) {
		this.tagsetIds.add(tagsetId);
	}

	public boolean isEmpty() {
		return this.collectionIds.isEmpty() 
				&& this.documentIds.isEmpty() 
				&& this.tagsetIds.isEmpty();
	}
}
