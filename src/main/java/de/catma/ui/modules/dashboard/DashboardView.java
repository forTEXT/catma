package de.catma.ui.modules.dashboard;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.UI;

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

	private final ProjectList projects;

    public DashboardView(ProjectManager projectManager, EventBus eventBus){
        this.projectManager = projectManager;
        this.eventBus = eventBus;
        this.errorLogger = (ErrorHandler)(UI.getCurrent());
        this.projects = new ProjectList(projectManager, eventBus);
        initComponents();
    }

    protected void initComponents() {
    	addStyleName("dashboard-view");
        
        CssLayout receivedResources = new CssLayout();
        receivedResources.setStyleName("flexlayout");
        
    
            
        DataProvider<ProjectReference,Void> projectDataProvider =
                DataProvider.fromCallbacks(
                        query -> {
                            try {
                            	int page = (query.getOffset() / query.getLimit()) + 1;
                            
                            	return projectManager.getProjectReferences()
                                    .page(page)
                                    .stream()
                                    ;
                            } catch (Exception e) {
                                errorLogger.showAndLogError("Can't get projects from ProjectManager",e);
                                return null;
                            }
                        },
                        query -> {
                            try {
                            	return projectManager.getProjectReferences().getTotalItems();
                            } catch (Exception e) {
                                errorLogger.showAndLogError("Can't get projects from ProjectManager",e);
                                return 0;
                            }
                        }
                );
           try {
			projectManager.getProjectReferences().page(1); //Fake call to satisfy vaadin 10 and above
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
        projects.setDataProvider(projectDataProvider);

        addComponents(projects, receivedResources);
    }

}
