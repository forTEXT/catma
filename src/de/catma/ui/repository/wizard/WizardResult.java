package de.catma.ui.repository.wizard;

import de.catma.core.document.source.ISourceDocument;
import de.catma.core.document.source.SourceDocumentInfo;

public class WizardResult {

	private SourceDocumentInfo sourceDocumentInfo;
	private ISourceDocument sourceDocument;
	private String sourceDocumentID = null;
	
	public WizardResult() {
		super();
		this.sourceDocumentInfo = new SourceDocumentInfo();
	}
	
	public SourceDocumentInfo getSourceDocumentInfo() {
		return sourceDocumentInfo;
	}
	
	public ISourceDocument getSourceDocument() {
		return sourceDocument;
	}
	
	public void setSourceDocument(ISourceDocument sourceDocument) {
		this.sourceDocument = sourceDocument;
	}
	
	public void setSourceDocumentID(String sourceDocumentID) {
		this.sourceDocumentID = sourceDocumentID;
	}
	
	public String getSourceDocumentID() {
		return sourceDocumentID;
	}
	
}
