package de.catma.ui.events;

import java.util.List;

import de.catma.project.Project;
import de.catma.queryengine.result.QueryResultRow;

public class QueryResultRowInAnnotateEvent {
	
	private final QueryResultRow selection;
	private final List<QueryResultRow> rows;
	private Project project;
	private String documentId;
	
	public QueryResultRowInAnnotateEvent(
			String documentId, QueryResultRow selection, 
			List<QueryResultRow> rows, Project project) {
		this.documentId = documentId;
		this.selection = selection;
		this.rows = rows;
		this.project = project;
	}
	public QueryResultRow getSelection() {
		return selection;
	}
	public List<QueryResultRow> getRows() {
		return rows;
	}
	
	public Project getProject() {
		return project;
	}
	
	public String getDocumentId() {
		return documentId;
	}
}
