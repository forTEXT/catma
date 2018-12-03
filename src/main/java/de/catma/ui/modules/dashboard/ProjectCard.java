package de.catma.ui.modules.dashboard;

import java.util.Objects;

import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.eventbus.EventBus;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.routing.RouteToProjectEvent;
import de.catma.ui.layout.FlexLayout;
import de.catma.ui.modules.main.ErrorLogger;

/**
 * Displays a single project reference as a card
 *
 * @author db
 */
public class ProjectCard extends VerticalLayout  {

    private ProjectReference item;

    private final ErrorLogger errorLogger;
    private final ProjectManager projectManager;

	private final EventBus eventbus;

    ProjectCard(ProjectReference t, ProjectManager projectManager, EventBus eventbus){
        this.item = Objects.requireNonNull(t) ;
        this.projectManager = projectManager;
        this.eventbus = eventbus;
        this.errorLogger = (ErrorLogger) UI.getCurrent();
        initComponents();
    }



    protected void initComponents() {
        addStyleName("projectlist__card");
//        setPadding(false);
        setSpacing(false);
        setWidth("352px");

        CssLayout preview = new CssLayout();
        preview.addStyleName("projectlist__card__preview");
        Label labelDesc = new Label(item.getDescription());
        labelDesc.setWidth("100%");
        preview.addComponents(labelDesc);
        Label labelProjectId = new Label(item.getProjectId());
        labelProjectId.setWidth("100%");
        preview.addComponents(labelProjectId);

        preview.addLayoutClickListener(evt -> eventbus.post(new RouteToProjectEvent(item)));
        addComponent(preview);

        FlexLayout descriptionBar = new FlexLayout();
        descriptionBar.addStyleName("projectlist__card__descriptionbar");
        descriptionBar.setAlignItems(FlexLayout.AlignItems.BASELINE);
        descriptionBar.setWidth("100%");
        
        
        Label name = new Label(item.getName());
        name.setWidth("100%");
        
        descriptionBar.addComponent(name);
//        descriptionBar.setExpandRatio(name,1.0f);

        IconButton buttonRemove = new IconButton(VaadinIcons.TRASH);
        descriptionBar.addComponents(buttonRemove);

        buttonRemove.addClickListener(
                (event -> {
                    ConfirmDialog.show(UI.getCurrent(),"delete Project",
                            "do you want to delete Project: " + item.getName(),
                            "OK",
                            "Cancel"
                    , (evt) -> {
                        try {
                            if(evt.isConfirmed()){
                            	projectManager.delete(item.getProjectId());                            	
                            }
                        } catch (Exception e) {
                            errorLogger.showAndLogError("can't delete project " + item.getName(), e);
                        }
                    });
                }
                ));
        IconButton buttonAction = new IconButton(VaadinIcons.ELLIPSIS_DOTS_V);
        descriptionBar.addComponents(buttonAction);

        addComponents(descriptionBar);
        
    }

}
