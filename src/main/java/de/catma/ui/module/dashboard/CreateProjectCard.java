package de.catma.ui.module.dashboard;

import java.util.Objects;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import de.catma.project.ProjectsManager;
import de.catma.ui.events.ProjectsChangedEvent;
import de.catma.ui.layout.VerticalFlexLayout;

/**
 * Renders a new project link styled as a card.
 * 
 * @author db
 *
 */
public class CreateProjectCard extends VerticalFlexLayout {

	private final ProjectsManager projectManager;
	private final EventBus eventBus;
	
	public CreateProjectCard(ProjectsManager projectManager, EventBus eventBus){
		this.projectManager = Objects.requireNonNull(projectManager);
        this.eventBus = eventBus;
        initComponents();

	}

	private void initComponents() {
		addStyleName("projectlist__newproject");

		CssLayout newProjectLayout = new CssLayout();
		newProjectLayout.addStyleName("projectlist__newproject__link");
		newProjectLayout.addLayoutClickListener(
				layoutClickEvent -> new CreateProjectDialog(
						projectManager,
						result -> eventBus.post(new ProjectsChangedEvent())
				).show()
		);

		Label labelDesc = new Label("create new project");
		labelDesc.setWidth("100%");
		newProjectLayout.addComponents(labelDesc);

		addComponent(newProjectLayout);
	}
}
