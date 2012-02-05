package de.catma.ui.repository.wizard;

import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.source.SourceDocumentInfo;

public class WizardResult {

	private SourceDocumentInfo sourceDocumentInfo;
	private SourceDocument sourceDocument;
	public WizardResult(SourceDocumentInfo sourceDocumentInfo) {
		super();
		this.sourceDocumentInfo = sourceDocumentInfo;
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
	
}
