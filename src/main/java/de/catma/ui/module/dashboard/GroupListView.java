package de.catma.ui.module.dashboard;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectsManager;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.GroupsChangedEvent;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;

public class GroupListView extends VerticalLayout {

    private final ProjectsManager projectsManager;
    private final EventBus eventBus;
    private final RemoteGitManagerRestricted remoteGitManagerRestricted;
    private final Set<Long> deletedGroupIds;
    private final HorizontalFlexLayout groupsLayout;
    private IconButton sortButton;
    private IconButton helpButton;


    public GroupListView(ProjectsManager projectsManager, EventBus eventBus, RemoteGitManagerRestricted remoteGitManagerRestricted) {
        this.projectsManager = projectsManager;
        this.eventBus = eventBus;
        this.remoteGitManagerRestricted = remoteGitManagerRestricted;

        this.deletedGroupIds = new HashSet<>();
        this.groupsLayout = new HorizontalFlexLayout();

        initComponents();
        eventBus.register(this);
        initData();
    }

    private void initData() {
        groupsLayout.removeAllComponents();
        groupsLayout.addComponent(new CreateGroupCard(projectsManager, eventBus));

        try {
            projectsManager.getGroups()
                    .stream()
                    // GitLab group deletion is a background operation, we usually still get the deleted group for some time after deletion
                    .filter(group -> !deletedGroupIds.contains(group.getId()))
//                    .sorted(selectedSortOrder)
                    .map(group -> new GroupCard(group, projectsManager, eventBus, remoteGitManagerRestricted))
                    .forEach(groupsLayout::addComponent);
        }
        catch (Exception e) {
            ((ErrorHandler) UI.getCurrent()).showAndLogError("Error accessing projects", e);
        }
    }

    private void initComponents() {

        addStyleName("groupslist");
        groupsLayout.addStyleNames("groupslist__list");

        setSizeFull();

        HorizontalLayout descriptionBar = new HorizontalLayout();

        Label title = new Label("Title");

        sortButton = new IconButton(VaadinIcons.ARROW_DOWN);

        descriptionBar.addComponent(title);
        descriptionBar.setComponentAlignment(title, Alignment.MIDDLE_RIGHT);
        descriptionBar.setExpandRatio(title, 1f);
        descriptionBar.addComponent(sortButton);
        descriptionBar.setComponentAlignment(sortButton, Alignment.MIDDLE_RIGHT);

        helpButton = new IconButton(VaadinIcons.QUESTION_CIRCLE);
        descriptionBar.addComponent(helpButton);
        descriptionBar.setComponentAlignment(helpButton, Alignment.MIDDLE_RIGHT);

        descriptionBar.setWidth("100%");

        addComponent(descriptionBar);

        addComponent(groupsLayout);
        setExpandRatio(groupsLayout, 1f);
    }
    
	@Subscribe
	public void handleProjectsChanged(GroupsChangedEvent groupsChangedEvent) {
		if (groupsChangedEvent.getDeletedGroupId() != null) {
			deletedGroupIds.add(groupsChangedEvent.getDeletedGroupId());
		}
		initData();
	}
	
	
	public void close() {
		eventBus.unregister(this);
	}}