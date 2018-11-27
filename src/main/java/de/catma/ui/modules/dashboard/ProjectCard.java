package de.catma.ui.modules.dashboard;

import java.util.Objects;

import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.component.IconButton;
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

    ProjectCard(ProjectReference t, ProjectManager projectManager){
        this.item = Objects.requireNonNull(t) ;
        this.projectManager = projectManager;
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
        preview.addComponents(new Label(item.getDescription()));
        preview.addComponents(new Label(item.getProjectId()));

        preview.addLayoutClickListener(evt -> {} );// TODO: open project));
        addComponent(preview);

        HorizontalLayout descriptionBar = new HorizontalLayout();
        
//        descriptionBar.setAlignItems(FlexComponent.Alignment.BASELINE);
        descriptionBar.setWidth("100%");

        Label name = new Label(item.getName());
        name.setWidth("100%");
        descriptionBar.addComponent(name);
        descriptionBar.setExpandRatio(name,1.0f);

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
