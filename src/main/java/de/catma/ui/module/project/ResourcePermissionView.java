package de.catma.ui.module.project;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.project.Project;
import de.catma.rbac.RBACRole;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.Member;

public class ResourcePermissionView extends Window {

    private final ErrorHandler errorHandler;
	private Grid<Resource> permissionGrid;
	private ActionGridComponent<Grid<Resource>> permissionGridComponent;

	private final Table<Resource,Member,RBACRole> permissionMatrix = HashBasedTable.create();
	
	private ListDataProvider<Resource> permissionData;
	private Project project;

	public ResourcePermissionView(
			List<Resource> documentResources, 
			Multimap<Resource,Resource> documentResourceToCollectionResources, 
			Project project) {
		
		this.project = project;
    	this.errorHandler = (ErrorHandler)UI.getCurrent();
        
		initComponents();
		initActions();
		initData(documentResources, documentResourceToCollectionResources);
	}

	
	private void initComponents() {
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		
		setCaption("Resource permission viewer");
		setWidth("85%");
		setHeight("90%");
		setModal(true);

		Label lDescription = new Label(
			"All resource specific permissions are listed below. "
			+ "You can only change resource specific roles not Project roles. "
			+ "Higher roles cannot be overwritten by lower roles.");
		lDescription.setWidth("100%");
		
		content.addComponent(lDescription);
		
		permissionGrid = new Grid<>();
        permissionGrid.setSizeFull();
        permissionGrid.setRowHeight(45);
        permissionGrid.setHeaderVisible(true);
        
        permissionGridComponent = 
    		new ActionGridComponent<Grid<Resource>> (
    			new Label("Resource specific permissions"),
    			permissionGrid);
        permissionGridComponent.setSizeFull();
        permissionGridComponent.setSelectionModeFixed(SelectionMode.SINGLE);
        permissionGridComponent.getActionGridBar().setMargin(new MarginInfo(false, false, false, true));
        
		content.addComponent(permissionGridComponent);
		content.setExpandRatio(permissionGridComponent, 1f);
		
		setContent(content);
	}
	
	private void initActions(){
		ContextMenu moreOptionsContextMenu = permissionGridComponent.getActionGridBar().getBtnMoreOptionsContextMenu();

		moreOptionsContextMenu.addItem(
				"Edit permission", 
				(click) -> {
					getSelectedResource().ifPresent(resource -> {
//						permissionGrid.getEditor().editRow(permiss);
						Notification.show("Info", "not yet implemented", Type.HUMANIZED_MESSAGE);
						
//						ResourcePermissionDialog rpd = new ResourcePermissionDialog(
//								resource,
//								project, 
//								(evt) -> initData());
//						rpd.addCloseListener(event -> initData());
//						rpd.show();
						
						
					});
				}	
		);

	}

	private void initData(
			List<Resource> documentResources, 
			Multimap<Resource, Resource> documentResourceToCollectionResources) {
    	try {
    		
    		permissionGrid.removeAllColumns();

    		permissionGrid
			.addColumn(entry -> entry.getName() + " / " + entry.getClass().getSimpleName().substring(0, 1))
			.setWidth(150)
			.setCaption("Resource");
    		
    		for (Resource documentResource : documentResources) {
    			Set<Member> documentMembers = 
    					project.getResourceMembers(documentResource.getResourceId());
    			documentMembers.forEach(
    				member -> permissionMatrix.put(documentResource, member, member.getRole()));
    			
    			Collection<Resource> collectionResources = 
    				documentResourceToCollectionResources.get(documentResource);
    			for (Resource collectionResource : collectionResources) {
    				Set<Member> collectionMembers = 
    						project.getResourceMembers(collectionResource.getResourceId());
    				collectionMembers.forEach(
    					member -> permissionMatrix.put(collectionResource, member, member.getRole()));
    			}
    			
    		}
    		
    		for (Member member : permissionMatrix.columnKeySet() ){
    			permissionGrid.addColumn(resource -> {
    				RBACRole role = permissionMatrix.get(resource, member);
    				return role==null?"":role.getRoleName();
    			})
    			.setExpandRatio(1)
    			.setCaption(member.getName())
    			.setDescriptionGenerator(resource -> member.preciseName());
    		}
    		
			permissionData = new ListDataProvider<>(permissionMatrix.rowKeySet());
			
			permissionGrid.setDataProvider(permissionData);
			
		} catch (IOException e) {
			errorHandler.showAndLogError("Failed to fetch permissions", e);
		}
	}
	
	public void show(){
		UI.getCurrent().addWindow(this);
	}
	
	
	private Optional<Resource> getSelectedResource() {
		Set<Resource> resources = permissionGrid.getSelectedItems();
		if (!resources.isEmpty()) {
			return Optional.of(resources.iterator().next());
		}
		else  {
			Notification.show("Info", "Please select a resource first!", Type.HUMANIZED_MESSAGE);
			return Optional.empty();
		}
	}

}
