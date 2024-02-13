package de.catma.ui.module.dashboard;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectReference;
import de.catma.project.ProjectsManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.GroupsChangedEvent;
import de.catma.ui.events.ProjectsChangedEvent;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;

/**
 * Displays a list of all projects in a card style layout.
 */
public class ProjectListView extends VerticalLayout {
	private final ProjectsManager projectsManager;
	private final EventBus eventBus;
	private final RemoteGitManagerRestricted remoteGitManagerRestricted;

	private final Set<String> deletedProjectIds;
	private final HorizontalFlexLayout projectsLayout;

	private final SortItem<ProjectReference> sortByNameAsc = new SortItem<ProjectReference>(
			new Comparator<ProjectReference>() {
				@Override
				public int compare(ProjectReference o1, ProjectReference o2) {
					return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
				}
			}, 
			"Name: A-Z");


	private final SortItem<ProjectReference> sortByNameDesc = new SortItem<ProjectReference>(
			new Comparator<ProjectReference>() {
				@Override
				public int compare(ProjectReference o1, ProjectReference o2) {
					return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()) * -1;
				}
			}, 
			"Name: Z-A");

	private final SortItem<ProjectReference> sortByCreatedAtAsc = new SortItem<ProjectReference>(
			new Comparator<ProjectReference>() {
				@Override
				public int compare(ProjectReference o1, ProjectReference o2) {
					return Objects.compare(o1.getCreatedAt(), o2.getCreatedAt(), LocalDate::compareTo) * -1;
				}
			}, 
			"Created: newest first");

	private final SortItem<ProjectReference> sortByCreatedAtDesc = new SortItem<ProjectReference>(
			new Comparator<ProjectReference>() {
				@Override
				public int compare(ProjectReference o1, ProjectReference o2) {
					return Objects.compare(o1.getCreatedAt(), o2.getCreatedAt(), LocalDate::compareTo);
				}
			}, 
			"Created: oldest first");
	
	private final SortItem<ProjectReference> sortByLastActivityAtAsc = new SortItem<ProjectReference>(
			new Comparator<ProjectReference>() {
				@Override
				public int compare(ProjectReference o1, ProjectReference o2) {
					return Objects.compare(o1.getLastActivityAt(), o2.getLastActivityAt(), LocalDate::compareTo) * -1;
				}
			}, 
			"Last activity: newest first");

	private final SortItem<ProjectReference> sortByLastActivityAtDesc = new SortItem<ProjectReference>(
			new Comparator<ProjectReference>() {
				@Override
				public int compare(ProjectReference o1, ProjectReference o2) {
					return Objects.compare(o1.getLastActivityAt(), o2.getLastActivityAt(), LocalDate::compareTo);
				}
			}, 
			"Last activity: oldest first");
	
	private ProjectManagerHelpWindow helpWindow;
	private IconButton helpButton;
	private ComboBox<SortItem<ProjectReference>> sortedByBox;
	private TextField searchField;

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
        sortedByBox.addSelectionListener(evt -> initData());
        
        helpButton.addClickListener(evt -> {
        	if (helpWindow.getParent() == null) {
        		UI.getCurrent().addWindow(helpWindow);
        	}
        	else {
        		UI.getCurrent().removeWindow(helpWindow);
        	}
        });
    	searchField.addValueChangeListener(valueChange -> initData());
	}

    private void initData() {
        projectsLayout.removeAllComponents();
        projectsLayout.addComponent(new CreateProjectCard(projectsManager, eventBus));
        projectsLayout.addComponent(new JoinProjectCard(remoteGitManagerRestricted.getUser(), eventBus));
        
        try {
	        projectsManager.getProjectReferences()
	        .stream()
	        .filter(projectRef -> searchField.getValue() == null || searchField.getValue().trim().isEmpty() || projectRef.getName().contains(searchField.getValue().trim()))
			// GitLab project deletion is a background operation, we usually still get the deleted project for some time after deletion
	        .filter(projectRef -> !deletedProjectIds.contains(projectRef.getProjectId()))
	        .sorted(sortedByBox.getValue().getSortComparator())
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
    	
    	HorizontalLayout actionBar = new HorizontalLayout();
    	
        searchField = new TextField();
        searchField.setPlaceholder("\u2315");
        searchField.addStyleName("project-list-view__search-field");
        
        actionBar.addComponent(searchField);
        actionBar.setComponentAlignment(searchField, Alignment.MIDDLE_RIGHT);
        actionBar.setExpandRatio(searchField, 1f);
        
        sortedByBox = new ComboBox<SortItem<ProjectReference>>(
        		"sorted by", 
        		Arrays.asList(
        				sortByNameAsc, sortByNameDesc, 
        				sortByCreatedAtAsc, sortByCreatedAtDesc, 
        				sortByLastActivityAtAsc, sortByLastActivityAtDesc));
        sortedByBox.setSelectedItem(sortByNameAsc);
        sortedByBox.setEmptySelectionAllowed(false);
        sortedByBox.addStyleName("project-list-view__sorted-by-box");

        actionBar.addComponent(sortedByBox);
		actionBar.setComponentAlignment(sortedByBox, Alignment.MIDDLE_RIGHT);
        
        helpButton = new IconButton(VaadinIcons.QUESTION_CIRCLE);
        actionBar.addComponent(helpButton);
        actionBar.setComponentAlignment(helpButton, Alignment.MIDDLE_RIGHT);

        actionBar.setWidth("100%");

        addComponent(actionBar);

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
	
	@Subscribe
	public void handleGroupsChanged(GroupsChangedEvent groupsChangedEvent) {
		initData();
	}
	
	public void close() {
		eventBus.unregister(this);
	}
}
