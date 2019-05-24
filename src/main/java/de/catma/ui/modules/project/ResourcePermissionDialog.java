package de.catma.ui.modules.project;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.rbac.RBACSubject;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.user.Member;

public class ResourcePermissionDialog extends Window {

    private final ErrorHandler errorHandler;
	private final IRemoteGitManagerRestricted remoteGitManager;
	private final Consumer<RBACSubject> onAssignmentCallback;
	private final Resource resource;
	private final VerticalLayout content = new VerticalLayout();
	private final Grid<Member> permissionGrid = new Grid<>();
	private final EventBus eventBus;
	private final ActionGridComponent<Grid<Member>> permissionGridComponent = new ActionGridComponent<Grid<Member>>(
			new Label("Permissions"),
			permissionGrid
			);

	private ListDataProvider<Member> permissionData;

	public ResourcePermissionDialog(EventBus eventBus, Resource resource, IRemoteGitManagerRestricted remoteGitManager, Consumer<RBACSubject> onAssignmentCallback ) {
		this.remoteGitManager = remoteGitManager;
		this.onAssignmentCallback = onAssignmentCallback.
				andThen((evt) -> eventBus.post(new ResourcesChangedEvent<Component>(this)));
		this.resource = resource;
    	this.errorHandler = (ErrorHandler)UI.getCurrent();
        this.eventBus = eventBus;
        
    	eventBus.register(this);

		initComponents();
		initActions();
		initData();
	}

	
	private void initComponents() {
		setCaption("Resource permission editor");
		setWidth("420px");
		setHeight("760px");
		setModal(true);
		content.addStyleName("spacing");
		content.addStyleName("margin");
		
		Label lDescription = new Label("All resource specific permission affecting the selected one are listed below."
				+ " Permissions at project level are not listed below."
				+ " If a member has a role with higher access level already assigned then it's not necessary to assign extra permissions.", ContentMode.HTML);
		lDescription.setWidth("100%");
		
		content.addComponent(lDescription);
		
		Label lAffectedResources = new Label("Affected resources: " + resource.getName());
		lAffectedResources.setWidth("100%");
//		lAffectedResources.setHeight("200px");
		
		content.addComponent(lAffectedResources);
		
//        permissionGrid.addStyleName("project-view-document-grid");
        permissionGrid.setHeaderVisible(false);
        permissionGrid.setWidth("100%");
        permissionGrid.setRowHeight(45);

		permissionGrid
			.addColumn(mem -> mem.getIdentifier(), new HtmlRenderer())
			.setExpandRatio(1)
			.setCaption("Username");
		
		permissionGrid
			.addColumn(mem -> mem.getRole().getRolename(), new HtmlRenderer())
			.setWidth(200)
			.setCaption("Role");
		
//		permissionGridComponent.addStyleName("project-view-action-grid");

		content.addComponent(permissionGridComponent);

		setContent(content);
	}
	
	private void initActions(){
		ContextMenu addContextMenu = permissionGridComponent.getActionGridBar().getBtnAddContextMenu();
		
		addContextMenu.addItem("add member", (click) -> 
		new CreateMemberDialog<>(
				resource,
				remoteGitManager::assignOnResource,
        		(query) -> remoteGitManager.getProjectMembers(resource.getProjectId()).stream().collect(Collectors.toList()),
				(rbacsubj) -> onAssignmentCallback.accept(rbacsubj)
				).show());
		
		ContextMenu moreOptionsContextMenu = permissionGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
		
		moreOptionsContextMenu.addItem("edit members", (click) -> new EditMemberDialog<>(
				resource,
				remoteGitManager::assignOnResource,
				permissionGrid.getSelectedItems(),
				(evt) -> eventBus.post(new ResourcesChangedEvent<Component>(this))
				).show());
		
		moreOptionsContextMenu.addItem("remove members", (click) -> new RemoveMemberDialog<>(
				resource,
				remoteGitManager::unassignFromResource,
				permissionGrid.getSelectedItems(),
				(evt) -> eventBus.post(new ResourcesChangedEvent<Component>(this))
				).show());
		
	}

	public void initData() {
    	try {
			permissionData = new ListDataProvider<>(remoteGitManager.getResourceMembers(resource));
			permissionGrid.setDataProvider(permissionData);
		} catch (IOException e) {
			errorHandler.showAndLogError("Failed to fetch permissions", e);
		}
	}
	
	public void show(){
		if (!this.isAttached()) {
			UI.getCurrent().addWindow(this);
		}
		else {
			this.bringToFront();
		}
	}
	
	
    /**
     * called when {@link ResourcesChangedEvent} is fired e.g. when source documents have been removed or added
     * @param resourcesChangedEvent
     */
    @Subscribe
    public void handleResourceChanged(ResourcesChangedEvent<TreeGrid<Resource>> resourcesChangedEvent){
    	initData();
    }
}
