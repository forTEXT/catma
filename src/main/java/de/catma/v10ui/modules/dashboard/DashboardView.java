package de.catma.v10ui.modules.dashboard;

import com.google.inject.Inject;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.data.provider.DataProvider;
import de.catma.Pager;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.v10ui.modules.main.ErrorLogger;

import java.util.List;

/**
 *
 * Renders a dashboard with all projects and all shared resources
 *
 * @author db
 */
public class DashboardView extends Composite<Div> {

    private final ProjectManager projectManager;

    private final ErrorLogger errorLogger;

    @Inject
    public DashboardView(ProjectManager projectManager, ErrorLogger errorLogger){
        this.projectManager = projectManager;
        this.errorLogger = errorLogger;
    }

    @Override
    protected Div initContent() {
        Div content = new Div();
        content.setClassName("dashboard-view");

        ProjectList projects = new ProjectList();
        FlexLayout receivedResources = new FlexLayout();

        try {
            Pager<ProjectReference> projectPager = this.projectManager.getProjectReferences();

            DataProvider<ProjectReference,Void> projectDataProvider =
                    DataProvider.fromCallbacks(
                            query -> {
                                int page = (query.getOffset() / query.getLimit()) + 1;
                                return projectPager
                                        .page(page)
                                        .stream()
                                        ;
                            },
                            query -> {
                                return projectPager.getTotalItems();
                            }
                    );
            List<ProjectReference> test = projectPager.page(1);
            projects.setDataProvider(projectDataProvider);

        } catch (Exception e) {
            errorLogger.showAndLogError("Can't get projects from ProjectManager",e);
            content.add(receivedResources);
        }

        content.add(projects,receivedResources);
        return content;
    }
}
