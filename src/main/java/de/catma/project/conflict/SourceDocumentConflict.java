package de.catma.project.conflict;

import de.catma.document.source.ContentInfoSet;

public class SourceDocumentConflict {
	private final String projectId;
	private final String sourceDocumentId;
	private final ContentInfoSet contentInfoSet;

	private boolean headerConflict;

	public SourceDocumentConflict(String projectId, String sourceDocumentId, ContentInfoSet contentInfoSet) {
		super();

		this.projectId = projectId;
		this.sourceDocumentId = sourceDocumentId;
		this.contentInfoSet = contentInfoSet;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getSourceDocumentId() {
		return sourceDocumentId;
	}

	public ContentInfoSet getContentInfoSet() {
		return contentInfoSet;
	}
	
	public void setHeaderConflict(boolean headerConflict) {
		this.headerConflict = headerConflict;
	}
	
	public boolean isHeaderConflict() {
		return headerConflict;
	}
}
