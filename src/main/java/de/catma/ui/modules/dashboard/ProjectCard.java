package de.catma.ui.modules.dashboard;

import java.util.Objects;

import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.eventbus.EventBus;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.layout.FlexLayout;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.modules.main.ErrorHandler;

/**
 * Displays a single project reference as a card
 *
 * @author db
 */
public class ProjectCard extends VerticalLayout  {

    private ProjectReference projectReference;

    private final ErrorHandler errorLogger;
    private final ProjectManager projectManager;

	private final EventBus eventbus;

    ProjectCard(ProjectReference projectReference, ProjectManager projectManager, EventBus eventbus){
        this.projectReference = Objects.requireNonNull(projectReference) ;
        this.projectManager = projectManager;
        this.eventbus = eventbus;
        this.errorLogger = (ErrorHandler) UI.getCurrent();
        initComponents();
    }



    protected void initComponents() {
        addStyleName("projectlist__card");

        CssLayout preview = new CssLayout();
        preview.addStyleName("projectlist__card__preview");
        Label labelDesc = new Label(projectReference.getDescription());
        labelDesc.setWidth("100%");
        preview.addComponents(labelDesc);
        Label labelProjectId = new Label(projectReference.getProjectId());
        labelProjectId.setWidth("100%");
        preview.addComponents(labelProjectId);

        preview.addLayoutClickListener(evt -> eventbus.post(new RouteToProjectEvent(projectReference)));
        addComponent(preview);

        HorizontalLayout descriptionBar = new HorizontalLayout();
        descriptionBar.addStyleName("projectlist__card__descriptionbar");
        descriptionBar.setAlignItems(FlexLayout.AlignItems.BASELINE);
        descriptionBar.setWidth("100%");
        
        
        Label name = new Label(projectReference.getName());
        name.setWidth("100%");
        
        descriptionBar.addComponent(name);
//        descriptionBar.setExpandRatio(name,1.0f);

        IconButton buttonRemove = new IconButton(VaadinIcons.TRASH);
        descriptionBar.addComponents(buttonRemove);

        buttonRemove.addClickListener(
                (event -> {
                    ConfirmDialog.show(UI.getCurrent(),"delete Project",
                            "do you want to delete Project: " + projectReference.getName(),
                            "OK",
                            "Cancel"
                    , (evt) -> {
                        try {
                            if(evt.isConfirmed()){
                            	projectManager.delete(projectReference.getProjectId());
                            	eventbus.post(new ResourcesChangedEvent<Component>(ProjectCard.this));
                            }
                        } catch (Exception e) {
                        	try {Thread.sleep(500);} catch (Exception e1) { } //TODO workaround gitlab race condition after delete. return old results if queried too fast
                            errorLogger.showAndLogError("can't delete project " + projectReference.getName(), e);
                        	eventbus.post(new ResourcesChangedEvent<Component>(ProjectCard.this));
                        }
                    });
                }
                ));
        IconButton buttonAction = new IconButton(VaadinIcons.ELLIPSIS_DOTS_V);
        descriptionBar.addComponents(buttonAction);

        addComponents(descriptionBar);
        
    }

    public String toString() {
    	return projectReference.getProjectId() + " " + projectReference.getName() + " "+ projectReference.getDescription();
    }
}
