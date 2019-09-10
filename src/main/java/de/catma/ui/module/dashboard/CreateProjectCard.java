package de.catma.ui.module.dashboard;

import java.util.Objects;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import de.catma.project.ProjectManager;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.layout.FlexLayout;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;

/**
 * Renders a new Project link styled as a card.
 * 
 * @author db
 *
 */
public class CreateProjectCard extends VerticalFlexLayout {

	private final ErrorHandler errorLogger;
	private final ProjectManager projectManager;
	private final EventBus eventBus;
	
	@Inject
	public CreateProjectCard(ProjectManager projectManager, EventBus eventBus){
		this.projectManager = Objects.requireNonNull(projectManager);
        this.errorLogger = (ErrorHandler) UI.getCurrent();
        this.eventBus = eventBus;
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

        HorizontalFlexLayout descriptionBar = new HorizontalFlexLayout();
        descriptionBar.addStyleName("projectlist__card__descriptionbar");
        descriptionBar.setAlignItems(FlexLayout.AlignItems.BASELINE);
        descriptionBar.setWidth("100%");

        addComponents(descriptionBar);
		
	}
}
