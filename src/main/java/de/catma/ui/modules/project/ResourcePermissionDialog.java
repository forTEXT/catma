package de.catma.ui.modules.project;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.document.repository.Repository;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.ui.rbac.RBACAssignmentFunction;
import de.catma.ui.rbac.RBACUnAssignmentFunction;
import de.catma.user.Member;

public class ResourcePermissionDialog extends Window {

    private final ErrorHandler errorHandler;
	private final Consumer<RBACSubject> onAssignmentCallback;
	private final Resource resource;
	private final VerticalFlexLayout content = new VerticalFlexLayout();
	private final Grid<Member> permissionGrid = new Grid<>();
	private final ActionGridComponent<Grid<Member>> permissionGridComponent = new ActionGridComponent<Grid<Member>>(
			new Label("Permissions"),
			permissionGrid
			);

	private ListDataProvider<Member> permissionData;
	private Repository project;

	public ResourcePermissionDialog(Resource resource, Repository project, Consumer<RBACSubject> onAssignmentCallback ) {
		this.project = project;
		this.onAssignmentCallback = onAssignmentCallback.
				andThen((evt) -> initData());
		this.resource = resource;
    	this.errorHandler = (ErrorHandler)UI.getCurrent();
        
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
		lDescription.addStyleNames("flexlayout");
		
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
		
		addContextMenu.addItem(
			"Add Member", 
			(click) -> 
				new CreateMemberDialog(
						new RBACAssignmentFunction() {
							@Override
							public RBACSubject assign(RBACSubject subject, RBACRole role) throws IOException {
								return project.assignOnResource(subject, role, resource.getResourceId());
							}
						},        		
						(query) -> project.getProjectMembers().stream().collect(Collectors.toList()),
						(rbacsubj) -> onAssignmentCallback.accept(rbacsubj)
				).show());
		
		ContextMenu moreOptionsContextMenu = permissionGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
		
		moreOptionsContextMenu.addItem(
				"Edit Members", 
				click -> new EditMemberDialog(
					new RBACAssignmentFunction() {
						@Override
						public RBACSubject assign(RBACSubject subject, RBACRole role) throws IOException {
							return project.assignOnResource(subject, role, resource.getResourceId());
						}
					},
					permissionGrid.getSelectedItems(),
					(evt) -> initData()
				).show());
		
		moreOptionsContextMenu.addItem(
				"Remove Members", 
				(click) -> new RemoveMemberDialog(
					new RBACUnAssignmentFunction() {
						@Override
						public void unassign(RBACSubject subject) throws IOException {
							project.unassignFromResource(subject, resource.getResourceId());
						}
						
					},				
					permissionGrid.getSelectedItems(),
					(evt) -> initData()
				).show());
	}

	public void initData() {
    	try {
			permissionData = new ListDataProvider<>(project.getResourceMembers(resource.getResourceId()));
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
