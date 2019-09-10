package de.catma.ui.module.project;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.project.Project;

public class CollectionResource implements Resource {

    private final AnnotationCollectionReference collectionReference;
	private final String projectId;
	private boolean hasWritePermission;

    public CollectionResource(AnnotationCollectionReference userMarkupCollectionReference, String projectId, boolean hasWritePermission){
        this.collectionReference = userMarkupCollectionReference;
        this.projectId = projectId;
        this.hasWritePermission = hasWritePermission;
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
	public String getPermissionIcon() {
		return hasWritePermission?VaadinIcons.UNLOCK.getHtml():VaadinIcons.LOCK.getHtml();
	}

}
