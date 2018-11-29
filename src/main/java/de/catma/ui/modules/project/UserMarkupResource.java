package de.catma.ui.modules.project;

import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

public class UserMarkupResource implements Resource {

    private final UserMarkupCollectionReference userMarkupCollectionReference;

    UserMarkupResource(UserMarkupCollectionReference userMarkupCollectionReference){
        this.userMarkupCollectionReference = userMarkupCollectionReference;
    }

    @Override
    public String detail() {
        return null;
    }

    @Override
    public boolean hasDetail() {
        return false;
    }

    @Override
    public String toString() {
        return userMarkupCollectionReference.toString();
    }
}
