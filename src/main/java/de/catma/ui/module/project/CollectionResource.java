package de.catma.ui.module.project;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.project.Project;
import de.catma.user.User;

public class CollectionResource implements Resource {

    private final AnnotationCollectionReference collectionReference;
	private final String projectId;
	private User responsibleUser;

    public CollectionResource(
    		AnnotationCollectionReference userMarkupCollectionReference, 
    		String projectId, User responsibleUser){
        this.collectionReference = userMarkupCollectionReference;
        this.projectId = projectId;
        this.responsibleUser = responsibleUser;
    }

    @Override
    public String getDetail() {
        return null;
    }

    @Override
    public boolean hasDetail() {
        return false;
    }
    
    @Override
    public String getName() {
        return collectionReference.toString();
    }
    
    @Override
    public String getIcon() {
		return VaadinIcons.NOTEBOOK.getHtml();
    }
    
    public AnnotationCollectionReference getCollectionReference() {
		return collectionReference;
	}
    
    @Override
    public String toString() {
    	return getName();
    }
    
    @Override
    public void deleteFrom(Project project) throws Exception {
    	project.delete(collectionReference);
    }
    
    @Override
    public boolean isCollection() {
    	return true;
    }

	@Override
	public String getResourceId() {
		return collectionReference.getId();
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
		return collectionReference.isResponsible(userIdentifier);
	}
	
	@Override
	public boolean isContribution() {
		return collectionReference.isContribution();
	}
}
