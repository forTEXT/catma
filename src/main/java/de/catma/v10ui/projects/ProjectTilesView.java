package de.catma.v10ui.projects;

import com.google.inject.Inject;
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
import de.catma.Pager;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;

import java.time.Instant;
import java.util.List;

@Tag("tilesView")
@HtmlImport("styles/tilesView-styles.html")
public class ProjectTilesView extends Div implements HasComponents {
    private ProjectReference projectReference;
    private ProjectManager projectManager;

    private VerticalLayout verticalLayout;
    private Label allProjectsLabel;
    private HorizontalLayout headerBar;
    private HorizontalLayout optionsBar;

    private Button titleArrowBt;


    @Inject
    public ProjectTilesView(ProjectManager projectManager) {

        this.projectManager = projectManager;
        setClassName("main_Content");

        initComponents();

    }

    private void initComponents(){

        createOptionsBar();
        createHeaderBar();

        verticalLayout = new VerticalLayout();
        verticalLayout.setClassName("verticalLayout");
        verticalLayout.add(optionsBar);
        verticalLayout.add(headerBar);

        try {
            Pager<ProjectReference> projectPager = this.projectManager.getProjectReferences();
            while (projectPager.hasNext()) {
                List<ProjectReference> projectRef = projectPager.next();
                HorizontalLayout gridRow = new HorizontalLayout();
                gridRow.setClassName("gridRow");

                for (ProjectReference project : projectRef) {

                    TileComponent tileComponent = new TileComponent("Project Details extra extra long",
                            project.getDescription(), project.getName());

                    gridRow.add(tileComponent);

                    if (gridRow.getElement().getChildCount() == 3) {
                        verticalLayout.add(gridRow);

                        gridRow = new HorizontalLayout();
                        gridRow.setClassName("gridRow");

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

   private void createHeaderBar(){
       headerBar= new HorizontalLayout();
       headerBar.setClassName("headerBar");

       allProjectsLabel = new Label("All projects" + Instant.now());

       Icon arrowUp = new Icon(VaadinIcon.ARROW_UP);
       titleArrowBt = new Button("Title");
       titleArrowBt.setIcon(arrowUp);
       titleArrowBt.setText("title");
       titleArrowBt.setClassName("title_Arrow");

       headerBar.add(allProjectsLabel,titleArrowBt);
       headerBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
    }

   private void  createOptionsBar(){
       Icon optionsIcon =  new Icon(VaadinIcon.OPTIONS);
       optionsIcon.setClassName("optionsIcon");

       optionsBar = new HorizontalLayout(optionsIcon);
       optionsBar.setClassName("projects_optionsbar");
       optionsBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
    }

    public ProjectReference getProjectReference() {
        return projectReference;
    }

    public void setProjectReference(ProjectReference projectReference) {
        this.projectReference = projectReference;
    }


}
