package de.catma.ui.modules.project;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.repository.Repository;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

public class CollectionResource implements Resource {

    private final UserMarkupCollectionReference collectionReference;

    CollectionResource(UserMarkupCollectionReference userMarkupCollectionReference){
        this.collectionReference = userMarkupCollectionReference;
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
    
    public UserMarkupCollectionReference getCollectionReference() {
		return collectionReference;
	}
    
    @Override
    public String toString() {
    	return getName();
    }
    
    @Override
    public void deleteFrom(Repository project) throws Exception {
    	project.delete(collectionReference);
    }
    
    @Override
    public boolean isCollection() {
    	return true;
    }
}
