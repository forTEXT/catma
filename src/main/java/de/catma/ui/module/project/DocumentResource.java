package de.catma.ui.module.project;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocumentReference;
import de.catma.project.Project;
import de.catma.user.User;

public class DocumentResource implements Resource {

    private final SourceDocumentReference sourceDocument;
	private final String projectId;
	private User responsibleUser;

    public DocumentResource(
    		SourceDocumentReference sourceDocument, 
    		String projectId, User responsibleUser){
        this.sourceDocument = sourceDocument;
        this.projectId = projectId;
        this.responsibleUser = responsibleUser;
    }
    
    @Override
    public String getDetail() {
        return sourceDocument.getSourceDocumentInfo().getContentInfoSet().getAuthor();
    }

    public SourceDocumentReference getDocument() {
		return sourceDocument;
	}
    
    @Override
    public boolean hasDetail() {
    	String author = 
    		sourceDocument.getSourceDocumentInfo().getContentInfoSet().getAuthor();
    	
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
		project.deleteSourceDocument(sourceDocument);
	}

	@Override
	public String getResourceId() {
		return sourceDocument.getUuid();
	}

	@Override
	public String getProjectId() {
		return projectId;
	}

	@Override
	public String getResponsibleUser() {
		return responsibleUser != null ? responsibleUser.getName() : "Not assigned";
	}
	
	@Override
	public boolean isResponsible(String userIdentifier) {
		
		for (AnnotationCollectionReference ref : sourceDocument.getUserMarkupCollectionRefs()) {
			if (!ref.isResponsible(userIdentifier)) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public boolean isContribution() {
		return false; // TODO
	}
}
