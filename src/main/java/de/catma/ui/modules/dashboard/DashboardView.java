package de.catma.ui.modules.dashboard;

import java.util.List;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.UI;

import de.catma.Pager;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.modules.main.ErrorHandler;

/**
 *
 * Renders a dashboard with all projects and all shared resources
 *
 * @author db
 */
public class DashboardView extends VerticalLayout {

    private final ProjectManager projectManager;

    private final ErrorHandler errorLogger;

	private final EventBus eventBus;

    public DashboardView(ProjectManager projectManager, EventBus eventBus){
        this.projectManager = projectManager;
        this.eventBus = eventBus;
        this.errorLogger = (ErrorHandler)(UI.getCurrent());
        initComponents();
    }

    protected void initComponents() {
        setStyleName("dashboard-view");
        
        ProjectList projects = new ProjectList(projectManager, eventBus);
        CssLayout receivedResources = new CssLayout();
        receivedResources.setStyleName("flexlayout");
        
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
            addComponent(receivedResources);
        }

        addComponents(projects, receivedResources);
    }
}
