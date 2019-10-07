package de.catma.ui.module.project;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.glassfish.jersey.internal.guava.Sets;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.project.Project;
import de.catma.rbac.RBACRole;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.Member;

public class ResourcePermissionView extends Window {

    private final ErrorHandler errorHandler;
	private final Multimap<Resource,Resource> resources;
	private final VerticalFlexLayout content = new VerticalFlexLayout();
	private final Grid<Map.Entry<Resource, Map<String, RBACRole>>> permissionGrid = new Grid<>();
	private final Table<Resource,String,RBACRole> permissionMatrix = HashBasedTable.<Resource, String, RBACRole>create();
	private final ActionGridComponent<Grid<Map.Entry<Resource, Map<String, RBACRole>>>> permissionGridComponent = 
			new ActionGridComponent<Grid<Map.Entry<Resource, Map<String, RBACRole>>>>(
			new Label("Permissions"),
			permissionGrid
			);
	
	private ListDataProvider<Map.Entry<Resource, Map<String, RBACRole>>> permissionData;
	private Project project;

	public ResourcePermissionView(Multimap<Resource,Resource> resources, Project project) {
		this.project = project;
		this.resources = resources;
    	this.errorHandler = (ErrorHandler)UI.getCurrent();
        
		initComponents();
		initActions();
		initData();
	}

	
	private void initComponents() {
		setCaption("Resource permission editor");
		setWidth("85%");
		setHeightUndefined();
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
//		permissionGrid
//			.addColumn(entry -> entry.getKey().getName() + " / " + entry.getKey().getClass().getSimpleName().substring(0, 1))
//			.setWidth(150)
//			.setCaption("Resource");
				
//		permissionGridComponent.addStyleName("project-view-action-grid");

		content.addComponent(permissionGridComponent);
//		content.addComponent(permissionGrid);
		setContent(content);
	}
	
	private void initActions(){
		ContextMenu moreOptionsContextMenu = permissionGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();

		moreOptionsContextMenu.addItem(
				"Edit permission", 
				(click) -> {
					ResourcePermissionDialog rpd = new ResourcePermissionDialog(
							getSelectedResource(),
							project, (evt) -> initData());
					rpd.addCloseListener(event -> initData());
					rpd.show();
				}	
		);

	}

	public void initData() {
    	try {
    		permissionGrid.removeAllColumns();

    		permissionGrid
			.addColumn(entry -> entry.getKey().getName() + " / " + entry.getKey().getClass().getSimpleName().substring(0, 1))
			.setWidth(150)
			.setCaption("Resource");

    		for (Entry<Resource, Resource> entry : resources.entries()) {
    			Set<Resource> res = Sets.newHashSet();
    			res.add(entry.getKey());
    			if(entry.getValue() != null){
    				res.add(entry.getValue());
    			}
    			for(Resource r : res) {
	    			Set<Member> members = project.getResourceMembers(r.getResourceId());
	    			for(Member member : members){    				
	    				permissionMatrix.put(r, member.getIdentifier(), member.getRole());
	    			}
    			}
			}
    		
    		for( String member : permissionMatrix.columnKeySet() ){
    			permissionGrid.addColumn(e -> {
    				Map<String, RBACRole> members = e.getValue();
    				if(members.containsKey(member)){
    					return members.get(member).getRoleName();
    				} else {
    					return "";
    				}
    			}, new HtmlRenderer())
    			.setExpandRatio(1)
    			.setCaption(member);
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
		final Set<Entry<Resource, Map<String, RBACRole>>> selectedResources = permissionGrid.getSelectedItems();
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

}
