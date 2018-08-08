package de.catma.v10ui.project;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import de.catma.Pager;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;

import java.util.List;

@Tag("projectMangerView")
public class ProjectManagerView extends Composite<Div> {
    private  H3 titleField = new H3();
    ProjectManager projectManager;
    Grid<ProjectReference> projectGrid;

    public ProjectManagerView(ProjectManager projectManager) {
        this.projectManager = projectManager;
        titleField = new H3("ProjectManager View");
        titleField.addClassName("main-layout__title");

/*
        try {
            List<ProjectReference> projectPager = (List<ProjectReference>) this.projectManager.getProjectReferences();

           // grid.addColumn(:getName).setHeader("Name");

        }catch(Exception e){
            e.printStackTrace();
        }*/

initComponents();
initData();

      getContent().add(titleField,projectGrid);

    }

  private void  initProjectManagerViewComponents(ProjectManager projectManager){

    }

    private void initComponents() {
        VerticalLayout leftPanel = new VerticalLayout();

        projectGrid = new Grid<ProjectReference>();
        projectGrid.setSizeFull();
        projectGrid.addColumn(ProjectReference::getName).setHeader("Name");
        projectGrid.addColumn(ProjectReference::getDescription).setHeader("Description");
/*
        HorizontalLayout buttonPanel = new HorizontalLayout();
        btCreateProject = new Button("Create Project");

        buttonPanel.addComponent(btCreateProject);


        btOpenProject = new Button("Open Project");
        buttonPanel.addComponent(btOpenProject);

        leftPanel.addComponent(projectGrid);
        leftPanel.addComponent(buttonPanel);

        addComponent(leftPanel);*/
    }

    private void initData() {
        try {
            Pager<ProjectReference> projectPager = this.projectManager.getProjectReferences();

            DataProvider<ProjectReference,Void> projectDataProvider =
                    DataProvider.fromCallbacks(
                            query -> {
                                int page = (query.getOffset() / query.getLimit())+1;

                                return projectPager
                                        .page(page)
                                        .stream();
                            },
                            query -> {

                                return projectPager.getTotalItems();
                            }
                    );
            projectGrid.setDataProvider(projectDataProvider);
        }
        catch (Exception e) {
            e.printStackTrace(); //TODO
        }
    }



}
