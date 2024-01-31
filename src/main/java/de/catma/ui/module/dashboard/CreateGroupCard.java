package de.catma.ui.module.dashboard;

import java.io.IOException;
import java.util.Objects;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

import de.catma.project.ProjectsManager;
import de.catma.ui.dialog.SingleTextInputDialog;
import de.catma.ui.events.GroupsChangedEvent;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;

/**
 * Renders a create-new-group-link styled as a card.
 * 
 * @author marco.petris@web.de
 *
 */
public class CreateGroupCard extends VerticalFlexLayout {

	private final ProjectsManager projectManager;
	private final EventBus eventBus;

	public CreateGroupCard(ProjectsManager projectManager, EventBus eventBus){
		this.projectManager = Objects.requireNonNull(projectManager);
        this.eventBus = eventBus;
        initComponents();

	}

	private void initComponents() {
		addStyleName("groupslist__newgroup");

		CssLayout newProjectLayout = new CssLayout();
		newProjectLayout.addStyleName("groupslist__newgroup__link");
		newProjectLayout.addLayoutClickListener(
				layoutClickEvent -> new SingleTextInputDialog(
						"Create Group",
						"Name",
						null,
						result -> {
							try {
								projectManager.createGroup(result);
								eventBus.post(new GroupsChangedEvent());
							} catch (IllegalArgumentException e) {
								Notification.show("Info", e.getMessage(), Type.HUMANIZED_MESSAGE);
							
							} catch (Exception e) {
								((ErrorHandler)UI.getCurrent()).showAndLogError(String.format("Failed to create group \"%s\"", result), e);
							}
						},
						new StringLengthValidator(
						        "Name must be between 2 and 50 characters long, please change the name accordingly!",
						        2, 50)
				).show()
		);

		Label labelDesc = new Label("create new group");
		labelDesc.setWidth("100%");
		newProjectLayout.addComponents(labelDesc);

		addComponent(newProjectLayout);
	}
}
