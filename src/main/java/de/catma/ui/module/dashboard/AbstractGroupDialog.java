package de.catma.ui.module.dashboard;

import com.google.common.base.Joiner;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.*;
import de.catma.project.ProjectsManager;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.Group;

import java.util.stream.Collectors;

/**
 * Dialog for creating/editing a group
 */
public abstract class AbstractGroupDialog extends AbstractOkCancelDialog<Group> {
    protected final ProjectsManager projectsManager;

    protected final ErrorHandler errorHandler;

    protected final Binder<GroupData> binder;
    protected final GroupData groupData;

    private final TextField name;
    private final TextArea description;

    class GroupData {
        private String name;
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public AbstractGroupDialog(String dialogCaption, SaveCancelListener<Group> saveCancelListener, ProjectsManager projectsManager) {
        super(dialogCaption, saveCancelListener);

        this.projectsManager = projectsManager;

        errorHandler = (ErrorHandler) UI.getCurrent();

        binder = new Binder<>();
        groupData = new GroupData();

        name = new TextField("Name");
        name.setWidth("100%");

        description = new TextArea("Description");
        description.setWidth("100%");
        description.setHeight("100%");

        binder.forField(name)
                .asRequired("Group name is required")
                .withValidator(new GroupNameValidator())
                .bind(GroupData::getName, GroupData::setName);
        binder.forField(description)
                .withValidator(new StringLengthValidator(
                        "Group description must not be longer than 500 characters",
                        0, 500
                ))
                .bind(GroupData::getDescription, GroupData::setDescription);
    }

    @Override
    protected void addContent(ComponentContainer content) {
        content.addComponent(name);
        content.addComponent(description);
        ((AbstractOrderedLayout) content).setExpandRatio(description, 1f);
    }

    @Override
    protected void handleOkPressed() {
        try {
            binder.writeBean(groupData);
            super.handleOkPressed();
        }
        catch (ValidationException e)
        {
            Notification.show(
                    Joiner.on("\n").join(
                            e.getValidationErrors().stream()
                                    .map(ValidationResult::getErrorMessage)
                                    .collect(Collectors.toList())
                    ),
                    Notification.Type.ERROR_MESSAGE
            );
        }
    }
}
