package de.catma.ui.module.dashboard;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.Group;

public class GroupListView extends VerticalLayout {

    private final ProjectsManager projectsManager;
    private final EventBus eventBus;
    private final RemoteGitManagerRestricted remoteGitManagerRestricted;
    private final Set<Long> deletedGroupIds;
    private final HorizontalFlexLayout groupsLayout;
	private ComboBox<SortItem<Group>> sortedByBox;
	private TextField searchField;
    private IconButton helpButton;

	private final SortItem<Group> sortByNameAsc = new SortItem<Group>(
			new Comparator<Group>() {
				@Override
				public int compare(Group o1, Group o2) {
					return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
				}
			}, 
			"Name: A-Z");


	private final SortItem<Group> sortByNameDesc = new SortItem<Group>(
			new Comparator<Group>() {
				@Override
				public int compare(Group o1, Group o2) {
					return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()) * -1;
				}
			}, 
			"Name: Z-A");
	
	private final SortItem<Group> sortByOwnedAsc;
	private final SortItem<Group> sortByOwnedDesc;

	private GroupManagerHelpWindow helpWindow;



    public GroupListView(ProjectsManager projectsManager, EventBus eventBus, RemoteGitManagerRestricted remoteGitManagerRestricted) {
        this.projectsManager = projectsManager;
        this.eventBus = eventBus;
        this.remoteGitManagerRestricted = remoteGitManagerRestricted;

        this.deletedGroupIds = new HashSet<>();
        this.groupsLayout = new HorizontalFlexLayout();

		this.sortByOwnedAsc = new SortItem<Group>(
				new Comparator<Group>() {
					@Override
					public int compare(Group o1, Group o2) {
						try {
							List<Long> ownedGroupIds = projectsManager.getOwnedGroupIds(false);
							if (ownedGroupIds.contains(o1.getId()) && !ownedGroupIds.contains(o2.getId())) {
								return -1;
							}
							else if (ownedGroupIds.contains(o2.getId()) && !ownedGroupIds.contains(o1.getId())) {
								return 1;
							}
							else return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()); 
 
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}, 
				"Owned groups first");

		this.sortByOwnedDesc = new SortItem<Group>(
				new Comparator<Group>() {
					@Override
					public int compare(Group o1, Group o2) {
						try {
							List<Long> ownedGroupIds = projectsManager.getOwnedGroupIds(false);
							if (ownedGroupIds.contains(o1.getId()) && !ownedGroupIds.contains(o2.getId())) {
								return 1;
							}
							else if (ownedGroupIds.contains(o2.getId()) && !ownedGroupIds.contains(o1.getId())) {
								return -1;
							}
							else return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName())*1; 
 
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}, 
				"Owned groups last");

        
        initComponents();
        eventBus.register(this);
        initData(false);
        initActions();
    }

    private void initActions() {
        sortedByBox.addSelectionListener(evt -> initData(false));
        
        helpButton.addClickListener(evt -> {
        	if (helpWindow.getParent() == null) {
        		UI.getCurrent().addWindow(helpWindow);
        	}
        	else {
        		UI.getCurrent().removeWindow(helpWindow);
        	}
        });
    	searchField.addValueChangeListener(valueChange -> initData(false));
	}

	private void initData(boolean forceReload) {
        groupsLayout.removeAllComponents();
        groupsLayout.addComponent(new CreateGroupCard(projectsManager, eventBus));

        try {
            projectsManager.getGroups(forceReload)
                    .stream()
        	        .filter(group -> searchField.getValue() == null || searchField.getValue().trim().isEmpty() || group.getName().contains(searchField.getValue().trim()))
                    // GitLab group deletion is a background operation, we usually still get the deleted group for some time after deletion
                    .filter(group -> !deletedGroupIds.contains(group.getId()))
        	        .sorted(sortedByBox.getValue().getSortComparator())
                    .map(group -> new GroupCard(group, projectsManager, eventBus, remoteGitManagerRestricted))
                    .forEach(groupsLayout::addComponent);
            
            if (forceReload) {
	        	projectsManager.getOwnedGroupIds(true);
            }
        }
        catch (Exception e) {
            ((ErrorHandler) UI.getCurrent()).showAndLogError("Error accessing projects", e);
        }
    }

    private void initComponents() {
    	helpWindow = new GroupManagerHelpWindow();
    	
        addStyleName("groupslist");
        groupsLayout.addStyleNames("groupslist__list");

        setSizeFull();

        HorizontalLayout actionBar = new HorizontalLayout();

        searchField = new TextField();
        searchField.setPlaceholder("\u2315");
        searchField.addStyleName("project-list-view__search-field");
        
        actionBar.addComponent(searchField);
        actionBar.setComponentAlignment(searchField, Alignment.MIDDLE_RIGHT);
        actionBar.setExpandRatio(searchField, 1f);
        
        sortedByBox = new ComboBox<SortItem<Group>>(
        		"sorted by", 
        		Arrays.asList(
        				sortByNameAsc, sortByNameDesc));
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

        addComponent(groupsLayout);
        setExpandRatio(groupsLayout, 1f);
    }
    
	@Subscribe
	public void handleProjectsChanged(GroupsChangedEvent groupsChangedEvent) {
		if (groupsChangedEvent.getDeletedGroupId() != null) {
			deletedGroupIds.add(groupsChangedEvent.getDeletedGroupId());
		}
		initData(true);
	}
	
	
	public void close() {
		eventBus.unregister(this);
	}}