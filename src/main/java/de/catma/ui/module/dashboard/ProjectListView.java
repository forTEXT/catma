package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import de.catma.project.ProjectReference;
import de.catma.project.ProjectsManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.ProjectsChangedEvent;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Displays a list of all projects in a card style layout.
 */
public class ProjectListView extends VerticalLayout {
	private final ProjectsManager projectsManager;
	private final EventBus eventBus;
	private final RemoteGitManagerRestricted remoteGitManagerRestricted;

	private final Set<String> deletedProjectIds;
	private final HorizontalFlexLayout projectsLayout;

	private final Comparator<ProjectReference> sortByNameAsc = new Comparator<ProjectReference>() {
		@Override
		public int compare(ProjectReference o1, ProjectReference o2) {
			return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
		}
	};
	private final Comparator<ProjectReference> sortByNameDesc = new Comparator<ProjectReference>() {
		@Override
		public int compare(ProjectReference o1, ProjectReference o2) {
			return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()) * -1;
		}
	};
	private Comparator<ProjectReference> selectedSortOrder = sortByNameAsc;

	private ProjectManagerHelpWindow helpWindow;
	private IconButton sortButton;
	private IconButton helpButton;

	public ProjectListView(ProjectsManager projectsManager, EventBus eventBus, RemoteGitManagerRestricted remoteGitManagerRestricted) {
		this.projectsManager = projectsManager;
		this.eventBus = eventBus;
		this.remoteGitManagerRestricted = remoteGitManagerRestricted;

		this.deletedProjectIds = new HashSet<>();
		this.projectsLayout = new HorizontalFlexLayout();

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
        projectsLayout.addComponent(new CreateProjectCard(projectsManager, eventBus));
        projectsLayout.addComponent(new JoinProjectCard(remoteGitManagerRestricted.getUser(), eventBus));
        
        try {
	        projectsManager.getProjectReferences()
	        .stream()
			// GitLab project deletion is a background operation, we usually still get the deleted project for some time after deletion
	        .filter(projectRef -> !deletedProjectIds.contains(projectRef.getProjectId()))
	        .sorted(selectedSortOrder)
	        .map(prj -> new ProjectCard(prj, projectsManager, eventBus, remoteGitManagerRestricted))
	        .forEach(projectsLayout::addComponent);
        }
        catch (Exception e) {
        	((ErrorHandler) UI.getCurrent()).showAndLogError("Error accessing projects", e);
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
	public void handleProjectsChanged(ProjectsChangedEvent projectsChangedEvent) {
		if (projectsChangedEvent.getDeletedProjectId() != null) {
			deletedProjectIds.add(projectsChangedEvent.getDeletedProjectId());
		}
		initData();
	}
}
