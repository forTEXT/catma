package de.catma.ui.module.project;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.source.SourceDocument;
import de.catma.project.Project;
import de.catma.repository.git.GitSourceDocumentHandler;

public class DocumentResource implements Resource {

    private final SourceDocument sourceDocument;
	private final String projectId;
	private boolean hasWritePermission;

    public DocumentResource(SourceDocument sourceDocument, String projectId, boolean hasWritePermission){
        this.sourceDocument = sourceDocument;
        this.projectId = projectId;
        this.hasWritePermission = hasWritePermission;
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
	public void deleteFrom(Project project) throws Exception {
		project.delete(sourceDocument);
	}

	@Override
	public String getResourceId() {
		return GitSourceDocumentHandler.getSourceDocumentRepositoryName(sourceDocument.getUuid());
	}

	@Override
	public String getProjectId() {
		return projectId;
	}

	@Override
	public String getPermissionIcon() {
		return hasWritePermission?VaadinIcons.UNLOCK.getHtml():VaadinIcons.LOCK.getHtml();
	}
}
