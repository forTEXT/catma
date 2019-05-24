package de.catma.ui.modules.project;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import org.glassfish.jersey.internal.guava.Sets;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.user.Member;

public class ResourcePermissionView extends Window {

    private final ErrorHandler errorHandler;
	private final IRemoteGitManagerRestricted remoteGitManager;
	private final Consumer<RBACSubject> onAssignmentCallback;
	private final Multimap<Resource,Resource> resources;
	private final VerticalLayout content = new VerticalLayout();
	private final Grid<Map.Entry<Resource, Map<Member, RBACRole>>> permissionGrid = new Grid<>();
	private final Table<Resource,Member,RBACRole> permissionMatrix = HashBasedTable.<Resource, Member, RBACRole>create();
	private final EventBus eventBus;
	private final ActionGridComponent<Grid<Map.Entry<Resource, Map<Member, RBACRole>>>> permissionGridComponent = 
			new ActionGridComponent<Grid<Map.Entry<Resource, Map<Member, RBACRole>>>>(
			new Label("Permissions"),
			permissionGrid
			);
	
	private ListDataProvider<Map.Entry<Resource, Map<Member, RBACRole>>> permissionData;

	public ResourcePermissionView(EventBus eventBus, Multimap<Resource,Resource> resources, IRemoteGitManagerRestricted remoteGitManager, Consumer<RBACSubject> onAssignmentCallback ) {
		this.remoteGitManager = remoteGitManager;
		this.onAssignmentCallback = onAssignmentCallback.
				andThen((evt) -> eventBus.post(new ResourcesChangedEvent<Component>(this)));
		this.resources = resources;
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
		
//		Label lAffectedResources = new Label("Affected resources: " + resource.getName());
//		lAffectedResources.setWidth("100%");
//		lAffectedResources.setHeight("200px");
		
//		content.addComponent(lAffectedResources);
		
//        permissionGrid.addStyleName("project-view-document-grid");
        permissionGrid.setHeaderVisible(false);
        permissionGrid.setWidth("100%");
        permissionGrid.setRowHeight(45);
        permissionGrid.setHeaderVisible(true);
		permissionGrid
			.addColumn(entry -> entry.getKey().getName() + " / " + entry.getKey().getClass().getSimpleName().substring(0, 1))
			.setWidth(150)
			.setCaption("Resource");
				
//		permissionGridComponent.addStyleName("project-view-action-grid");

		content.addComponent(permissionGridComponent);
//		content.addComponent(permissionGrid);
		setContent(content);
	}
	
	private void initActions(){
		ContextMenu addContextMenu = permissionGridComponent.getActionGridBar().getBtnAddContextMenu();
//		
//		addContextMenu.addItem("add member", (click) -> 
//		new CreateMemberDialog<>(
//				resource,
//				remoteGitManager::assignOnResource,
//        		(query) -> remoteGitManager.getProjectMembers(resource.getProjectId()).stream().collect(Collectors.toList()),
//				(rbacsubj) -> onAssignmentCallback.accept(rbacsubj)
//				).show());
//		
		ContextMenu moreOptionsContextMenu = permissionGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();
//		
//		moreOptionsContextMenu.addItem("edit members", (click) -> new EditMemberDialog<>(
//				resource,
//				remoteGitManager::assignOnResource,
//				permissionGrid.getSelectedItems(),
//				(evt) -> eventBus.post(new ResourcesChangedEvent<Component>(this))
//				).show());
//		
//		moreOptionsContextMenu.addItem("remove members", (click) -> new RemoveMemberDialog<>(
//				resource,
//				remoteGitManager::unassignFromResource,
//				permissionGrid.getSelectedItems(),
//				(evt) -> eventBus.post(new ResourcesChangedEvent<Component>(this))
//				).show());
	
		moreOptionsContextMenu.addItem("Edit permission", (click) -> new ResourcePermissionDialog(
				eventBus, 
				getSelectedResource() ,
				remoteGitManager, (evt) -> eventBus.post(new ResourcesChangedEvent<Component>(this))
			).show());

	}

	public void initData() {
    	try {
    		for (Entry<Resource, Resource> entry : resources.entries()) {
    			Set<Resource> res = Sets.newHashSet();
    			res.add(entry.getKey());
    			if(entry.getValue() != null){
    				res.add(entry.getValue());
    			}
    			for(Resource r : res) {
	    			Set<Member> members = remoteGitManager.getResourceMembers(r);
	    			for(Member member : members){    				
	    				permissionMatrix.put(r, member, member.getRole());
	    			}
    			}
			}
    		for( Member member : permissionMatrix.columnKeySet() ){
    			permissionGrid.addColumn(e -> {
    				Map<Member, RBACRole> members = e.getValue();
    				if(members.containsKey(member)){
    					return members.get(member).roleName;
    				} else {
    					return "";
    				}
    			}, new HtmlRenderer())
    			.setExpandRatio(1)
    			.setCaption(member.getIdentifier());
    		}
			permissionData = new ListDataProvider<>(permissionMatrix.rowMap().entrySet());
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
	
	
  private Resource getSelectedResource(){
		final Set<Entry<Resource, Map<Member, RBACRole>>> selectedResources = permissionGrid.getSelectedItems();
		if ((selectedResources.size() != 1) 
				&& !selectedResources.iterator().next().getKey().isCollection()) {
			Notification.show("Info", "Please select a single entry first!", Type.HUMANIZED_MESSAGE);
			return null;
		}	
		else {
			final Resource resource = selectedResources.iterator().next().getKey();
			return resource;
		}
  }

    /**
     * called when {@link ResourcesChangedEvent} is fired e.g. when source documents have been removed or added
     * @param resourcesChangedEvent
     */
    @Subscribe
    public void handleResourceChanged(ResourcesChangedEvent<TreeGrid<Resource>> resourcesChangedEvent){
    //	initData();
    }
}
