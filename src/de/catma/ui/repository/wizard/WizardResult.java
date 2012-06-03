package de.catma.ui.repository.wizard;

import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentInfo;

public class WizardResult {

	private SourceDocumentInfo sourceDocumentInfo;
	private SourceDocument sourceDocument;
	private String sourceDocumentID = null;
	
	public WizardResult() {
		super();
		this.sourceDocumentInfo = new SourceDocumentInfo();
	}
	
	public SourceDocumentInfo getSourceDocumentInfo() {
		return sourceDocumentInfo;
	}
	
	public SourceDocument getSourceDocument() {
		return sourceDocument;
	}
	
	public void setSourceDocument(SourceDocument sourceDocument) {
		this.sourceDocument = sourceDocument;
	}
	
	public void setSourceDocumentID(String sourceDocumentID) {
		this.sourceDocumentID = sourceDocumentID;
	}
	
	public String getSourceDocumentID() {
		return sourceDocumentID;
	}
	
}
