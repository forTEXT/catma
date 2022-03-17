package de.catma.ui.module.project;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.project.Project;
import de.catma.user.User;

public class CollectionResource implements Resource {

    private final AnnotationCollectionReference collectionReference;
	private final String projectId;
	private User responsableUser;

    public CollectionResource(
    		AnnotationCollectionReference userMarkupCollectionReference, 
    		String projectId, User responsableUser){
        this.collectionReference = userMarkupCollectionReference;
        this.projectId = projectId;
        this.responsableUser = responsableUser;
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
	public String getResponsableUser() {
		return responsableUser != null ? responsableUser.getName() : "Not assigned";
	}
}
