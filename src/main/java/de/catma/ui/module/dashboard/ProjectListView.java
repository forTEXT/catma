package de.catma.ui.module.dashboard;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.rbac.IRBACManager;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.ProjectChangedEvent;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;

/**
 * Displays a list of all projects in a card style layout.
 * @author db
 */
public class ProjectListView extends VerticalLayout {

    private final ProjectManager projectManager;
	private final EventBus eventBus;
	private final Comparator<ProjectReference> sortByNameAsc = (ref1,ref2) -> ref1.getName().compareTo(ref2.getName());
	private final Comparator<ProjectReference> sortByNameDesc = (ref1,ref2) -> ref2.getName().compareTo(ref1.getName());
	private final IRBACManager rbacManager;
	private final Provider<JoinProjectCard> joinProjectCardProvider;
	private Comparator<ProjectReference> selectedSortOrder = sortByNameAsc;
	private final Set<String> deletedProjectIds;

	@Inject
    ProjectListView(@Assisted("projectManager")ProjectManager projectManager, 
    		EventBus eventBus, 
    		IRBACManager rbacManager, 
    		Provider<JoinProjectCard> joinProjectCardProvider){ 
        this.projectManager = projectManager;
        this.eventBus = eventBus;
        this.rbacManager = rbacManager;
        this.joinProjectCardProvider = joinProjectCardProvider;
        this.deletedProjectIds = new HashSet<String>();
        initComponents();
        eventBus.register(this);
        initData();
    }

    private HorizontalFlexLayout projectsLayout = new HorizontalFlexLayout();

    private void initData() {
        projectsLayout.removeAllComponents();
        projectsLayout.addComponent(new CreateProjectCard(projectManager, eventBus));
        projectsLayout.addComponent(joinProjectCardProvider.get());
        try {
	        projectManager.getProjectReferences()
	        .stream()
	        .filter(projectRef -> !deletedProjectIds.contains(projectRef.getProjectId())) // gitlab group removal is a background operation, we usually still get the removed Project immediately after removal
	        .sorted(selectedSortOrder)
	        .map(prj -> new ProjectCard(prj, projectManager, eventBus, rbacManager))
	        .forEach(projectsLayout::addComponent);
        }
        catch (Exception e) {
        	((ErrorHandler)UI.getCurrent()).showAndLogError("Error accessing Projects", e);
        }
    }

    protected void initComponents() {
    	addStyleName("projectlist");
    	projectsLayout.addStyleNames("projectlist__list");
    	
    	setSizeFull();
    	
    	HorizontalLayout descriptionBar = new HorizontalLayout();
        Label description = new Label("All Projects");

        Label title = new Label("Title");

        IconButton sortButton = new IconButton(VaadinIcons.ARROW_DOWN);
        
        sortButton.addClickListener((evt) -> {
        	if(sortButton.getIcon().equals(VaadinIcons.ARROW_DOWN)){
        		selectedSortOrder=sortByNameDesc;
        		sortButton.setIcon(VaadinIcons.ARROW_UP);
        	}else {
        		selectedSortOrder=sortByNameAsc;
        		sortButton.setIcon(VaadinIcons.ARROW_DOWN);
        	}
        	initData();
        });

        descriptionBar.addComponent(description);
        descriptionBar.setExpandRatio(description, 1f);
        descriptionBar.addComponent(title);
        descriptionBar.addComponent(sortButton);
        descriptionBar.setComponentAlignment(sortButton, Alignment.MIDDLE_RIGHT);

        descriptionBar.setWidth("100%");

        addComponent(descriptionBar);

        addComponent(projectsLayout);
        setExpandRatio(projectsLayout, 1f);
    }

	@Subscribe
	public void resourceChanged(ProjectChangedEvent projectChangedEvent) {
		if (projectChangedEvent.getDeletedProjectId() != null) {
			deletedProjectIds.add(projectChangedEvent.getDeletedProjectId());
		}
		initData();
	}
}
