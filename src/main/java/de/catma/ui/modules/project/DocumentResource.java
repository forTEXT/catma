package de.catma.ui.modules.project;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.source.SourceDocument;

public class DocumentResource implements Resource {

    private final SourceDocument sourceDocument;

    public DocumentResource(SourceDocument sourceDocument){
        this.sourceDocument = sourceDocument;
    }
    
    @Override
    public String getDetail() {
        return sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet().getAuthor();
    }

    public SourceDocument getDocument() {
		return sourceDocument;
	}
    
    @Override
    public boolean hasDetail() {
        return true;
    }

    @Override
    public String getName() {
        return sourceDocument.toString();
    }
    
    @Override
    public String getIcon() {
		return VaadinIcons.BOOK.getHtml();
    }
}
