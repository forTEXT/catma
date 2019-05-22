package de.catma.project.conflict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.catma.document.source.ContentInfoSet;

public class CollectionConflict {
	private List<AnnotationConflict> annotationConflicts;
	private String projectId;
	private String collectionId;
	private ContentInfoSet contentInfoSet;
	private String sourceDocumentId;
	
	public CollectionConflict(String projectId, String collectionId, 
			ContentInfoSet contentInfoSet, String sourceDocumentId) {
		this.projectId = projectId;
		this.collectionId = collectionId;
		this.contentInfoSet = contentInfoSet;
		this.sourceDocumentId = sourceDocumentId;
		this.annotationConflicts = new ArrayList<>();
	}
	
	public void addAnnotationConflict(AnnotationConflict annotationConflict) {
		this.annotationConflicts.add(annotationConflict);
	}
	
	public List<AnnotationConflict> getAnnotationConflicts() {
		return Collections.unmodifiableList(annotationConflicts);
	}

	public String getProjectId() {
		return projectId;
	}

	public String getCollectionId() {
		return collectionId;
	}

	public ContentInfoSet getContentInfoSet() {
		return contentInfoSet;
	}
	
	public String getDocumentId() {
		return sourceDocumentId;
	}
}
