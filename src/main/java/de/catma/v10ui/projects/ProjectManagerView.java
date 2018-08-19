package de.catma.v10ui.projects;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import de.catma.Pager;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.v10ui.frame.FrameView;

@Tag("projectMangerView")
@ParentLayout(FrameView.class)
public class ProjectManagerView extends Composite<Div> {
    private  H3 titleField = new H3();
    ProjectManager projectManager;
    Grid<ProjectReference> projectGrid;

    public ProjectManagerView(ProjectManager projectManager) {
        this.projectManager = projectManager;
        initComponents();
        initData();
        getContent().add(projectGrid);
        getContent().addClassName("main-layout_projects");
    }

    private void initComponents() {
        VerticalLayout leftPanel = new VerticalLayout();
        projectGrid = new Grid<ProjectReference>();
        projectGrid.setSizeFull();
        projectGrid.addColumn(ProjectReference::getName).setHeader("Name");
        projectGrid.addColumn(ProjectReference::getDescription).setHeader("Description");
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
