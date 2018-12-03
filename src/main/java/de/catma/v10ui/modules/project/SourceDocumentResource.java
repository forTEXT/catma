package de.catma.v10ui.modules.project;

import de.catma.document.source.SourceDocument;

public class SourceDocumentResource implements Resource {

    private final SourceDocument sourceDocument;

    public SourceDocumentResource(SourceDocument sourceDocument){
        this.sourceDocument = sourceDocument;
    }
    @Override
    public String detail() {
        return sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet().getAuthor();
    }

    @Override
    public boolean hasDetail() {
        return true;
    }

    @Override
    public String toString() {
        return sourceDocument.toString();
    }
}
