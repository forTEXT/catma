package de.catma.ui.module.dashboard;

import com.vaadin.ui.ComponentContainer;
import de.catma.project.ProjectsManager;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.user.Group;

import java.io.IOException;

public class EditGroupDialog extends AbstractGroupDialog {
    private final Group group;

    public EditGroupDialog(ProjectsManager projectsManager, Group group, SaveCancelListener<Group> saveCancelListener) {
        super("Edit User Group", saveCancelListener, projectsManager);

        this.group = group;
    }

    @Override
    protected void addContent(ComponentContainer content) {
        super.addContent(content);

        groupData.setName(group.getName());
        groupData.setDescription(group.getDescription());
        binder.readBean(groupData);
    }

    @Override
    protected Group getResult() {
        try {
            return projectsManager.updateGroup(groupData.getName(), groupData.getDescription(), group);
        }
        catch (IOException e) {
            errorHandler.showAndLogError("Failed to update user group", e);
            return null;
        }
    }
}
