package de.catma.ui.modules.dashboard;

import java.util.Objects;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import de.catma.project.ProjectManager;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.layout.FlexLayout;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.modules.main.ErrorHandler;

/**
 * Renders a new Project link styled as a card.
 * 
 * @author db
 *
 */
public class CreateProjectCard extends VerticalLayout {

	private final ErrorHandler errorLogger;
	private final ProjectManager projectManager;
	private final EventBus eventBus;

	
	public CreateProjectCard(ProjectManager projectManager, EventBus eventBus){
		this.projectManager = Objects.requireNonNull(projectManager);
		this.eventBus = Objects.requireNonNull(eventBus);
        this.errorLogger = (ErrorHandler) UI.getCurrent();
        initComponents();

	}

	private void initComponents() {
        addStyleName("projectlist__newproject");

        CssLayout newproject = new CssLayout();
        newproject.addStyleName("projectlist__newproject__link");
        Label labelDesc = new Label("create new project");
        labelDesc.setWidth("100%");
        newproject.addComponents(labelDesc);

        newproject.addLayoutClickListener(evt -> {
        	new CreateProjectDialog(projectManager, result -> eventBus.post(new ResourcesChangedEvent<Component>(this))).show();


        });
        addComponent(newproject);

        HorizontalLayout descriptionBar = new HorizontalLayout();
        descriptionBar.addStyleName("projectlist__card__descriptionbar");
        descriptionBar.setAlignItems(FlexLayout.AlignItems.BASELINE);
        descriptionBar.setWidth("100%");

        addComponents(descriptionBar);
		
	}
}
