package de.catma.ui.module.dashboard;

import de.catma.project.ProjectsManager;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.user.Group;

import java.io.IOException;

public class CreateGroupDialog extends AbstractGroupDialog {
    public CreateGroupDialog(ProjectsManager projectsManager, SaveCancelListener<Group> saveCancelListener) {
        super("Create User Group", saveCancelListener, projectsManager);
    }

    @Override
    protected Group getResult() {
        try {
            return projectsManager.createGroup(groupData.getName(), groupData.getDescription());
        }
        catch (IOException e) {
            errorHandler.showAndLogError("Failed to create user group", e);
            return null;
        }
    }
}
