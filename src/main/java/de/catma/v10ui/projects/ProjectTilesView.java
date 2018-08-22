package de.catma.v10ui.projects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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

@Tag("tileView")
@HtmlImport("styles/tileView-styles.html")
public class ProjectTilesView extends Div implements HasComponents {
    private ProjectReference projectReference;
    private ProjectManager projectManager;

    private VerticalLayout verticalLayout;
    private HorizontalLayout headerBar;
    private HorizontalLayout optionsBar;

    private Button titleArrow;


    public ProjectTilesView(ProjectManager projectManager) {

        getElement().getStyle().set("margin-left","0");
        getElement().getStyle().set("padding-left","0");

        setClassName("main_Content");
        this.projectManager = projectManager;
        verticalLayout = new VerticalLayout();

        verticalLayout.getStyle().set("margin","0");
        verticalLayout.getStyle().set("padding","0");
        verticalLayout.getStyle().set("background-color", "#e6e6e6");

        headerBar= new HorizontalLayout();
        Label allProjectsLabel = new Label("All projects");

        Icon arrowUp = new Icon(VaadinIcon.ARROW_UP);
        titleArrow = new Button("Title");
        titleArrow.setIcon(arrowUp);
        titleArrow.setText("title");
        titleArrow.getStyle().set("margin-right","0");
        titleArrow.getStyle().set("background-color","inherited");
        headerBar.add(allProjectsLabel,titleArrow);
        headerBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        headerBar.getElement().getStyle().set("width","100%");
        Icon optionsIcon =  new Icon(VaadinIcon.OPTIONS);
        optionsIcon.getElement().getStyle().set("margin-top","20px");
        optionsIcon.getElement().getStyle().set("margin-right","10px");
        optionsBar = new HorizontalLayout(optionsIcon);

        optionsBar.setClassName("projects_optionsbar");
        optionsBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

         verticalLayout.add(optionsBar);
         verticalLayout.add(headerBar);






        try {
            Pager<ProjectReference> projectPager = this.projectManager.getProjectReferences();
            while (projectPager.hasNext()) {
                List<ProjectReference> projectRef = projectPager.next();
                HorizontalLayout gridRow = new HorizontalLayout();
                gridRow.setClassName("gridRow");
        /*        gridRow.getElement().getStyle().set("width","auto");
                gridRow.getElement().getStyle().set("margin-left","25px");*/

                for (ProjectReference project : projectRef) {

                    TileComponent tileComponent = new TileComponent("Project Details extra extra long", project.getDescription(), project.getName());
            /*        tileComponent.getElement().getStyle().set("width","31%");
                    tileComponent.getElement().getStyle().set("border-radius", "5px");
                    tileComponent.getElement().getStyle().set("padding","20px");*/


                    gridRow.add(tileComponent);

                    if (gridRow.getElement().getChildCount() == 3) {
                        verticalLayout.add(gridRow);

                        gridRow = new HorizontalLayout();
                        gridRow.setClassName("gridRow");

             /*           gridRow.getElement().getStyle().set("width","100%");
                        gridRow.getElement().getStyle().set("margin-left","25px");*/
                    }else{
                        verticalLayout.add(gridRow);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        add(verticalLayout);


    }

    public ProjectReference getProjectReference() {
        return projectReference;
    }

    public void setProjectReference(ProjectReference projectReference) {
        this.projectReference = projectReference;
    }


}
