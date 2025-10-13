package de.catma.ui.module.dashboard;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import de.catma.project.ProjectsManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.GroupsChangedEvent;
import de.catma.ui.events.MembersChangedEvent;
import de.catma.ui.events.RefreshEvent;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.Group;

import java.util.*;

/**
 * Displays a list of all groups in a card style layout.
 */
public class GroupListView extends VerticalLayout {
	private final ProjectsManager projectsManager;
	private final EventBus eventBus;
	private final RemoteGitManagerRestricted remoteGitManagerRestricted;

	private final SortItem<Group> sortByNameAsc = new SortItem<>(
			(o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()),
			"Name: A-Z"
	);

	private final SortItem<Group> sortByNameDesc = new SortItem<>(
			(o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()) * -1,
			"Name: Z-A"
	);

	private final SortItem<Group> sortByOwnedAsc = new SortItem<>(
			new Comparator<>() {
				@Override
				public int compare(Group o1, Group o2) {
					if (ownedGroupIds.contains(o1.getId()) && !ownedGroupIds.contains(o2.getId())) {
						return -1;
					}
					else if (ownedGroupIds.contains(o2.getId()) && !ownedGroupIds.contains(o1.getId())) {
						return 1;
					}
					else {
						return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
					}
				}
			},
			"Owned groups first"
	);

	private final SortItem<Group> sortByOwnedDesc = new SortItem<>(
			new Comparator<>() {
				@Override
				public int compare(Group o1, Group o2) {
					if (ownedGroupIds.contains(o1.getId()) && !ownedGroupIds.contains(o2.getId())) {
						return 1;
					}
					else if (ownedGroupIds.contains(o2.getId()) && !ownedGroupIds.contains(o1.getId())) {
						return -1;
					}
					else {
						return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
					}
				}
			},
			"Owned groups last"
	);

	private TextField searchField;
	private ComboBox<SortItem<Group>> sortedByBox;
	private IconButton helpButton;
	private GroupManagerHelpWindow helpWindow;
	private HorizontalFlexLayout groupsLayout;

	private List<Long> ownedGroupIds;

	public GroupListView(ProjectsManager projectsManager, EventBus eventBus, RemoteGitManagerRestricted remoteGitManagerRestricted) {
		this.projectsManager = projectsManager;
		this.eventBus = eventBus;
		this.remoteGitManagerRestricted = remoteGitManagerRestricted;

		initComponents();
		initData(false);
		initActions();

		eventBus.register(this);
	}

	private void initComponents() {
		addStyleName("groupslist");

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
				Arrays.asList(sortByNameAsc, sortByNameDesc, sortByOwnedAsc, sortByOwnedDesc)
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

		helpWindow = new GroupManagerHelpWindow();

		groupsLayout = new HorizontalFlexLayout();
		groupsLayout.addStyleNames("groupslist__list");
		addComponent(groupsLayout);
		setExpandRatio(groupsLayout, 1f);
	}

	private void initData(boolean forceReload) {
		groupsLayout.removeAllComponents();

		groupsLayout.addComponent(new CreateGroupCard(projectsManager, eventBus));

		try {
			ownedGroupIds = projectsManager.getOwnedGroupIds(forceReload);

			projectsManager.getGroups(forceReload)
					.stream()
					.filter(
							group -> searchField.getValue() == null || searchField.getValue().trim().isEmpty()
									|| group.getName().contains(searchField.getValue().trim())
					)
					.sorted(sortedByBox.getValue().getSortComparator())
					.map(group -> new GroupCard(group, projectsManager, eventBus, remoteGitManagerRestricted))
					.forEach(groupsLayout::addComponent);
		}
		catch (Exception e) {
			((ErrorHandler) UI.getCurrent()).showAndLogError("Failed to fetch groups", e);
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
	public void handleGroupsChanged(GroupsChangedEvent groupsChangedEvent) {
		initData(true);
	}

	@Subscribe
	public void handleMembersChanged(MembersChangedEvent membersChangedEvent) {
		initData(true);
	}

	@Subscribe
	public void refresh(RefreshEvent refreshEvent) {
		initData(true);
	}

	public void close() {
		eventBus.unregister(this);
	}}
