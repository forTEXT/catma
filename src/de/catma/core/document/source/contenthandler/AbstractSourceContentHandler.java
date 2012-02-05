package de.catma.core.document.source.contenthandler;

import de.catma.core.document.source.SourceDocumentInfo;

public abstract class AbstractSourceContentHandler implements SourceContentHandler {

    private SourceDocumentInfo sourceDocumentInfo;
    private String content;
    
    public void setSourceDocumentInfo(SourceDocumentInfo sourceDocumentInfo) {
		this.sourceDocumentInfo = sourceDocumentInfo;
	}
    
    public SourceDocumentInfo getSourceDocumentInfo() {
		return sourceDocumentInfo;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

    
}
