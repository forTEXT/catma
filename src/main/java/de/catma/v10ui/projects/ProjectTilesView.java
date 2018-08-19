package de.catma.v10ui.projects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import de.catma.Pager;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.v10ui.frame.FrameView;

import java.util.List;

@Tag("tile")
@HtmlImport("styles/projecttile-style.html")
public class ProjectTilesView extends Composite<Div> implements HasComponents {
    private ProjectReference projectReference;
    private ProjectManager projectManager;

    private VerticalLayout verticalLayout;


    public ProjectTilesView(ProjectManager projectManager) {

        this.projectManager = projectManager;
        verticalLayout = new VerticalLayout();


        verticalLayout.getStyle().set("border", "1px solid #9E9E9E");

        verticalLayout.getStyle().set("background-color", "white");

        try {
            Pager<ProjectReference> projectPager = this.projectManager.getProjectReferences();
            while (projectPager.hasNext()) {
                List<ProjectReference> projectRef = projectPager.next();
                HorizontalLayout gridRow = new HorizontalLayout();
                gridRow.getElement().getStyle().set("width","100%");

                for (ProjectReference project : projectRef) {

                    TileComponent tileComponent = new TileComponent("Project Details", project.getDescription(), project.getName());
                    tileComponent.getElement().getStyle().set("width","33%");
                    tileComponent.getElement().getStyle().set("border-radius", "5px");

                    gridRow.add(tileComponent);

                    if (gridRow.getElement().getChildCount() == 3) {
                        verticalLayout.add(gridRow);

                        gridRow = new HorizontalLayout();
                        gridRow.getElement().getStyle().set("width","100%");
                    }else{
                        verticalLayout.add(gridRow);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
