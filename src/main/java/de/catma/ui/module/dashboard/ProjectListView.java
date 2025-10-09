package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import de.catma.project.ProjectReference;
import de.catma.project.ProjectsManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.GroupsChangedEvent;
import de.catma.ui.events.ProjectsChangedEvent;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Displays a list of all projects in a card style layout.
 */
public class ProjectListView extends VerticalLayout {
	private final ProjectsManager projectsManager;
	private final EventBus eventBus;
	private final RemoteGitManagerRestricted remoteGitManagerRestricted;

	private final Set<String> deletedProjectIds;

	private final SortItem<ProjectReference> sortByNameAsc = new SortItem<>(
			(o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()),
			"Name: A-Z"
	);

	private final SortItem<ProjectReference> sortByNameDesc = new SortItem<>(
			(o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()) * -1,
			"Name: Z-A"
	);

	private final SortItem<ProjectReference> sortByCreatedAtAsc = new SortItem<>(
			(o1, o2) -> Objects.compare(o1.getCreatedAt(), o2.getCreatedAt(), ZonedDateTime::compareTo) * -1,
			"Created: newest first"
	);

	private final SortItem<ProjectReference> sortByCreatedAtDesc = new SortItem<>(
			(o1, o2) -> Objects.compare(o1.getCreatedAt(), o2.getCreatedAt(), ZonedDateTime::compareTo),
			"Created: oldest first"
	);
	
	private final SortItem<ProjectReference> sortByLastActivityAtAsc = new SortItem<>(
			(o1, o2) -> Objects.compare(o1.getLastActivityAt(), o2.getLastActivityAt(), ZonedDateTime::compareTo) * -1,
			"Last activity: newest first"
	);

	private final SortItem<ProjectReference> sortByLastActivityAtDesc = new SortItem<>(
			(o1, o2) -> Objects.compare(o1.getLastActivityAt(), o2.getLastActivityAt(), ZonedDateTime::compareTo),
			"Last activity: oldest first"
	);

	private final SortItem<ProjectReference> sortByOwnedAsc = new SortItem<>(
			new Comparator<>() {
				@Override
				public int compare(ProjectReference o1, ProjectReference o2) {
					try {
						List<String> ownedProjectIds = projectsManager.getOwnedProjectIds(false);
						if (ownedProjectIds.contains(o1.getProjectId()) && !ownedProjectIds.contains(o2.getProjectId())) {
							return -1;
						}
						else if (ownedProjectIds.contains(o2.getProjectId()) && !ownedProjectIds.contains(o1.getProjectId())) {
							return 1;
						}
						else {
							return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
						}
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			},
			"Owned projects first"
	);

	private final SortItem<ProjectReference> sortByOwnedDesc = new SortItem<>(
			new Comparator<>() {
				@Override
				public int compare(ProjectReference o1, ProjectReference o2) {
					try {
						List<String> ownedProjectIds = projectsManager.getOwnedProjectIds(false);
						if (ownedProjectIds.contains(o1.getProjectId()) && !ownedProjectIds.contains(o2.getProjectId())) {
							return 1;
						}
						else if (ownedProjectIds.contains(o2.getProjectId()) && !ownedProjectIds.contains(o1.getProjectId())) {
							return -1;
						}
						else {
							return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
						}
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			},
			"Owned projects last"
	);

	private TextField searchField;
	private ComboBox<SortItem<ProjectReference>> sortedByBox;
	private IconButton helpButton;
	private ProjectManagerHelpWindow helpWindow;
	private HorizontalFlexLayout projectsLayout;

	public ProjectListView(ProjectsManager projectsManager, EventBus eventBus, RemoteGitManagerRestricted remoteGitManagerRestricted) {
		this.projectsManager = projectsManager;
		this.eventBus = eventBus;
		this.remoteGitManagerRestricted = remoteGitManagerRestricted;

		this.deletedProjectIds = new HashSet<>();

		initComponents();
		initData(false);
		initActions();

		eventBus.register(this);
	}

	protected void initComponents() {
		addStyleName("projectlist");

		HorizontalLayout actionBar = new HorizontalLayout();
		actionBar.setWidth("100%");

		searchField = new TextField();
		searchField.setPlaceholder("⌕");
		searchField.addStyleName("project-list-view__search-field");

		actionBar.addComponent(searchField);
		actionBar.setComponentAlignment(searchField, Alignment.MIDDLE_RIGHT);
		actionBar.setExpandRatio(searchField, 1f);

		sortedByBox = new ComboBox<>(
				"sorted by",
				Arrays.asList(
						sortByNameAsc, sortByNameDesc,
						sortByCreatedAtAsc, sortByCreatedAtDesc,
						sortByLastActivityAtAsc, sortByLastActivityAtDesc,
						sortByOwnedAsc, sortByOwnedDesc
				)
		);
		sortedByBox.setSelectedItem(sortByNameAsc);
		sortedByBox.setEmptySelectionAllowed(false);
		sortedByBox.addStyleName("project-list-view__sorted-by-box");

		actionBar.addComponent(sortedByBox);
		actionBar.setComponentAlignment(sortedByBox, Alignment.MIDDLE_RIGHT);

		helpButton = new IconButton(VaadinIcons.QUESTION_CIRCLE);
		actionBar.addComponent(helpButton);
		actionBar.setComponentAlignment(helpButton, Alignment.MIDDLE_RIGHT);

		addComponent(actionBar);

		helpWindow = new ProjectManagerHelpWindow();

		projectsLayout = new HorizontalFlexLayout();
		projectsLayout.addStyleNames("projectlist__list");
		addComponent(projectsLayout);
		setExpandRatio(projectsLayout, 1f);
	}

	private void initData(boolean forceReload) {
		projectsLayout.removeAllComponents();

		projectsLayout.addComponent(new CreateProjectCard(projectsManager, eventBus));
		projectsLayout.addComponent(new JoinProjectCard(remoteGitManagerRestricted.getUser(), eventBus));

		try {
			projectsManager.getProjectReferences(forceReload)
					.stream()
					.filter(
							projectRef -> searchField.getValue() == null || searchField.getValue().trim().isEmpty()
									|| projectRef.getName().contains(searchField.getValue().trim())
					)
					// GitLab project deletion is a background operation, we usually still get the deleted project for some time after deletion
					// TODO: check if this is still relevant now that GitLab marks projects for deletion and only truly deletes them later
					//       also see ProjectApi.getProjects calls in GitlabManagerRestricted (now also filtering on 'active')
					//       projects marked for deletion are renamed (unlike groups), so this filter no longer works (what we call the project ID is actually
					//       the path, we don't use the stable/fixed numeric ID)
					.filter(projectRef -> !deletedProjectIds.contains(projectRef.getProjectId()))
					.sorted(sortedByBox.getValue().getSortComparator())
					.map(projectRef -> new ProjectCard(projectRef, projectsManager, eventBus, remoteGitManagerRestricted))
					.forEach(projectsLayout::addComponent);

			if (forceReload) {
				projectsManager.getOwnedProjectIds(true);
			}
		}
		catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Failed to fetch projects", e);
		}
	}

	private void initActions() {
		searchField.addValueChangeListener(valueChangeEvent -> initData(false));

		sortedByBox.addSelectionListener(singleSelectionEvent -> initData(false));

		helpButton.addClickListener(clickEvent -> {
			if (helpWindow.getParent() == null) {
				UI.getCurrent().addWindow(helpWindow);
			}
			else {
				UI.getCurrent().removeWindow(helpWindow);
			}
		});
	}

	@Subscribe
	public void handleProjectsChanged(ProjectsChangedEvent projectsChangedEvent) {
		if (projectsChangedEvent.getDeletedProjectId() != null) {
			deletedProjectIds.add(projectsChangedEvent.getDeletedProjectId());
		}
		initData(true);
	}

	@Subscribe
	public void handleGroupsChanged(GroupsChangedEvent groupsChangedEvent) {
		initData(true);
	}

	public void close() {
		eventBus.unregister(this);
	}
}
