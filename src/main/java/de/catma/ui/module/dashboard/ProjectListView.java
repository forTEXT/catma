package de.catma.ui.module.dashboard;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectsManager;
import de.catma.project.ProjectReference;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.ProjectChangedEvent;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;

/**
 * Displays a list of all projects in a card style layout.
 * @author db
 */
public class ProjectListView extends VerticalLayout {

    private final ProjectsManager projectManager;
	private final EventBus eventBus;
	private final Comparator<ProjectReference> sortByNameAsc = 
			(ref1,ref2) -> String.CASE_INSENSITIVE_ORDER.compare(ref1.getName(), ref2.getName());
	private final Comparator<ProjectReference> sortByNameDesc = 
			(ref1,ref2) -> String.CASE_INSENSITIVE_ORDER.compare(ref1.getName(), ref2.getName())*-1;
	private final RemoteGitManagerRestricted remoteGitManagerRestricted;
	private Comparator<ProjectReference> selectedSortOrder = sortByNameAsc;
	private final Set<String> deletedProjectIds;
	private HorizontalFlexLayout projectsLayout = new HorizontalFlexLayout();
	private IconButton sortButton;
	private IconButton helpButton;
	private ProjectManagerHelpWindow helpWindow;

    public ProjectListView(ProjectsManager projectManager, 
    		EventBus eventBus, 
    		RemoteGitManagerRestricted remoteGitManagerRestricted){
        this.projectManager = projectManager;
        this.eventBus = eventBus;
        this.remoteGitManagerRestricted = remoteGitManagerRestricted;
        this.deletedProjectIds = new HashSet<String>();
        initComponents();
        initActions();
        eventBus.register(this);
        initData();
    }

    private void initActions() {
        
        sortButton.addClickListener(evt -> {
        	if(sortButton.getIcon().equals(VaadinIcons.ARROW_DOWN)){
        		selectedSortOrder=sortByNameDesc;
        		sortButton.setIcon(VaadinIcons.ARROW_UP);
        	}else {
        		selectedSortOrder=sortByNameAsc;
        		sortButton.setIcon(VaadinIcons.ARROW_DOWN);
        	}
        	initData();
        });		
        
        helpButton.addClickListener(evt -> {
        	if (helpWindow.getParent() == null) {
        		UI.getCurrent().addWindow(helpWindow);
        	}
        	else {
        		UI.getCurrent().removeWindow(helpWindow);
        	}
        });
	}


    private void initData() {
        projectsLayout.removeAllComponents();
        projectsLayout.addComponent(new CreateProjectCard(projectManager, eventBus));
        projectsLayout.addComponent(new JoinProjectCard(remoteGitManagerRestricted.getUser(), eventBus));
        
        try {
	        projectManager.getProjectReferences()
	        .stream()
	        .filter(projectRef -> !deletedProjectIds.contains(projectRef.getProjectId())) // gitlab group removal is a background operation, we usually still get the removed Project immediately after removal
	        .sorted(selectedSortOrder)
	        .map(prj -> new ProjectCard(prj, projectManager, eventBus, remoteGitManagerRestricted))
	        .forEach(projectsLayout::addComponent);
        }
        catch (Exception e) {
        	((ErrorHandler)UI.getCurrent()).showAndLogError("Error accessing Projects", e);
        }
    }

    protected void initComponents() {
    	helpWindow = new ProjectManagerHelpWindow();
    	
    	addStyleName("projectlist");
    	projectsLayout.addStyleNames("projectlist__list");
    	
    	setSizeFull();
    	
    	HorizontalLayout descriptionBar = new HorizontalLayout();
        Label description = new Label("All Projects");

        Label title = new Label("Title");

        sortButton = new IconButton(VaadinIcons.ARROW_DOWN);
        descriptionBar.addComponent(description);
        descriptionBar.setExpandRatio(description, 1f);
        descriptionBar.addComponent(title);
        descriptionBar.addComponent(sortButton);
        descriptionBar.setComponentAlignment(sortButton, Alignment.MIDDLE_RIGHT);
        
        helpButton = new IconButton(VaadinIcons.QUESTION_CIRCLE);
        descriptionBar.addComponent(helpButton);
        descriptionBar.setComponentAlignment(helpButton, Alignment.MIDDLE_RIGHT);

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
