package de.catma.ui.module.dashboard;

import java.util.Objects;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import de.catma.project.ProjectsManager;
import de.catma.ui.events.GroupsChangedEvent;
import de.catma.ui.layout.VerticalFlexLayout;

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

		addLayoutClickListener(
				layoutClickEvent -> new CreateGroupDialog(
						projectManager,
						result -> eventBus.post(new GroupsChangedEvent())
				).show()
		);

		CssLayout newProjectLayout = new CssLayout();
		newProjectLayout.addStyleName("groupslist__newgroup__link");

		Label labelDesc = new Label("create new user group");
		labelDesc.setWidth("100%");
		newProjectLayout.addComponents(labelDesc);

		addComponent(newProjectLayout);
	}
}
