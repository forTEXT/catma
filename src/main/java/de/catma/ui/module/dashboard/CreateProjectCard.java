package de.catma.ui.module.dashboard;

import java.util.Objects;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.Page;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;

import de.catma.project.ProjectManager;
import de.catma.ui.events.ProjectChangedEvent;
import de.catma.ui.layout.FlexLayout;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;

/**
 * Renders a new Project link styled as a card.
 * 
 * @author db
 *
 */
public class CreateProjectCard extends VerticalFlexLayout {

	private final ProjectManager projectManager;
	private final EventBus eventBus;
	
	public CreateProjectCard(ProjectManager projectManager, EventBus eventBus){
		this.projectManager = Objects.requireNonNull(projectManager);
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
//            new CreateProjectDialog(projectManager, result -> eventBus.post(new ProjectChangedEvent())).show();
            Notification projectCreationDisabledNotification = new Notification(
                    "New project creation is disabled in CATMA 6, please use CATMA 7!",
                    Notification.Type.WARNING_MESSAGE
            );
            projectCreationDisabledNotification.setDelayMsec(-1);
            projectCreationDisabledNotification.show(Page.getCurrent());
        });
        addComponent(newproject);

        HorizontalFlexLayout descriptionBar = new HorizontalFlexLayout();
        descriptionBar.addStyleName("projectlist__card__descriptionbar");
        descriptionBar.setAlignItems(FlexLayout.AlignItems.BASELINE);
        descriptionBar.setWidth("100%");

        addComponents(descriptionBar);
		
	}
}
