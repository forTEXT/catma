package de.catma.ui.modules.project;

import com.aliasi.util.Strings;
import com.vaadin.icons.VaadinIcons;

import de.catma.document.repository.Repository;
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
    	String author = 
    		sourceDocument.getSourceContentHandler().getSourceDocumentInfo().getContentInfoSet().getAuthor();
    	
    	return author != null && !author.trim().isEmpty();
    }

    @Override
    public String getName() {
        return sourceDocument.toString();
    }
    
    @Override
    public String getIcon() {
		return VaadinIcons.BOOK.getHtml();
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sourceDocument == null) ? 0 : sourceDocument.hashCode());
		return result;
	}
	
	@Override
	public String toString() {
		return getName();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DocumentResource other = (DocumentResource) obj;
		if (sourceDocument == null) {
			if (other.sourceDocument != null)
				return false;
		} else if (!sourceDocument.equals(other.sourceDocument))
			return false;
		return true;
	}
    
	@Override
	public void deleteFrom(Repository project) throws Exception {
		project.delete(sourceDocument);
	}
    
}
