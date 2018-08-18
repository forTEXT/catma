package de.catma.v10ui.projects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import de.catma.Pager;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;

import java.util.List;

@Tag("tile")
public class ProjectTilesView extends Composite<Div> implements HasComponents {
    private ProjectReference projectReference;
    private ProjectManager projectManager;

    private VerticalLayout verticalLayout;


    // composite ist zum erstellen einer neuen Componenet aus anderen Vaadin (oder von mir erstellten Components) Components wie zb Textfield oder Label
    // die werden dem root-div mit getcontent.add hinzugetan, so wie auch css definitionen
    public ProjectTilesView(String projectTitle, ProjectManager projectManager) {

        this.projectManager = projectManager;
        verticalLayout = new VerticalLayout();


        verticalLayout.getStyle().set("border", "1px solid #9E9E9E");


        // verticalLayout.setHeight("300px");
        verticalLayout.getStyle().set("background-color", "grey");

        try {
            Pager<ProjectReference> projectPager = this.projectManager.getProjectReferences();
            while (projectPager.hasNext()) {
                List<ProjectReference> projectRef = projectPager.next();
                HorizontalLayout gridRow = new HorizontalLayout();
                for (ProjectReference project : projectRef) {


                    gridRow.add(new TileComponent("Project Details", project.getDescription(), project.getName()));

                    if (gridRow.getElement().getChildCount() == 3) {
                        verticalLayout.add(gridRow);

                        gridRow = new HorizontalLayout();
                    }else{
                        verticalLayout.add(gridRow);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        // verticalLayout.add(firstTile,secondTile);

        add(verticalLayout);
        getContent().addClassName("main-layout_tile");

    }

    public ProjectReference getProjectReference() {
        return projectReference;
    }

    public void setProjectReference(ProjectReference projectReference) {
        this.projectReference = projectReference;
    }


}
