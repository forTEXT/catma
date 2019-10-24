package de.catma.ui.module.project;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.vaadin.data.Binder;
import com.vaadin.data.Binder.Binding;
import com.vaadin.data.StatusChangeEvent;
import com.vaadin.data.StatusChangeListener;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.HtmlRenderer;

import de.catma.project.Project;
import de.catma.rbac.RBACRole;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.component.TreeGridFactory;
import de.catma.ui.component.actiongrid.ActionGridComponent;
import de.catma.ui.component.actiongrid.SearchFilterProvider;
import de.catma.ui.dialog.HelpWindow;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.Member;

public class ResourcePermissionView extends Window {

	private final ErrorHandler errorHandler;
	private TreeGrid<Resource> permissionGrid;
	private ActionGridComponent<TreeGrid<Resource>> permissionGridComponent;

	private final Table<Resource,Member,RBACRole> permissionMatrix = HashBasedTable.create();
	
	private TreeDataProvider<Resource> permissionDataProvider;
	private Project project;

	public ResourcePermissionView(
			TreeData<Resource> resourceData, 
			Collection<TagsetDefinition> tagsets, 
			Project project) {
		
		this.project = project;
    	this.errorHandler = (ErrorHandler)UI.getCurrent();
        
		initComponents();
		initActions();
		initData(resourceData, tagsets);
	}

	
	private void initComponents() {
		HorizontalLayout content = new HorizontalLayout();
		content.setSizeFull();
		content.setMargin(new MarginInfo(true, false, true, true));
		content.setSpacing(false);
		
		setCaption("Resource permissions");
		setWidth("85%");
		setHeight("90%");
		setModal(true);
		
		permissionGrid = TreeGridFactory.createDefaultTreeGrid();
        permissionGrid.setSizeFull();
        permissionGrid.setRowHeight(45);
        permissionGrid.setHeaderVisible(true);
        
        permissionGrid.getEditor().setEnabled(true);
        permissionGrid.getEditor().setBuffered(false);
        
        permissionGridComponent = 
    		new ActionGridComponent<TreeGrid<Resource>> (
    			new Label("Resource specific permissions"),
    			permissionGrid);
        permissionGridComponent.setSizeFull();
        permissionGridComponent.setSelectionModeFixed(SelectionMode.SINGLE);
        permissionGridComponent.getActionGridBar().setMargin(new MarginInfo(false, false, false, true));
        permissionGridComponent.getActionGridBar().setMoreOptionsBtnVisible(false);
        
		content.addComponent(permissionGridComponent);
		content.setExpandRatio(permissionGridComponent, 1f);
		
		
		HelpWindow helpWindow = new HelpWindow("Resource permissions", 
				"<p>The resource permission matrix gives you an overview of the resource specific permissions for each team member.</p>"
				+ "<p>By default it shows team members with changeable resource roles only. But you can add columns for all team members by using the show/hide columns menu in the upper right corner of the table</p>"
				+ "<p>You can change resource specific roles only. Double click on a resource row to set a new resource specific role for a team member. To change Project roles go to the team section of the Project view.</p>"
				+ "<p>Note that higher roles on the Project level cannot be overwritten by lower roles on the resource level. In fact it is not even possible to set lower roles in such cases.</p>");

		Button btHelp = helpWindow.createHelpWindowButton();
		content.addComponent(btHelp);
		content.setComponentAlignment(btHelp, Alignment.TOP_RIGHT);
		

		setContent(content);
	}
	
	private void initActions() {
		permissionGridComponent.setSearchFilterProvider(
			new SearchFilterProvider<Resource>() {
				@Override
				public SerializablePredicate<Resource> createSearchFilter(String searchInput) {
					return new SerializablePredicate<Resource>() {
						@Override
						public boolean test(Resource t) {
							if (t != null) {
								if (t.getName().toLowerCase().startsWith(searchInput)) {
									return true;
								}
								List<Resource> children = 
									permissionDataProvider.getTreeData().getChildren(t);
								
								for (Resource child : children) {
									if (test(child)) {
										return true;
									}
								}
							}
							return false;						}
					};
				}
			});
	}

	private void initData(
			TreeData<Resource> resourceData, 
			Collection<TagsetDefinition> tagsets) {
    	try {
    		
    		permissionGrid.removeAllColumns();

    		permissionGrid
			.addColumn(resource -> resource.getIcon(), new HtmlRenderer())
			.setWidth(70);
    		
    		permissionGrid
			.addColumn(resource -> resource.getName())
			.setWidth(250)
			.setCaption("Resource");
    		TreeData<Resource> items = new TreeData<Resource>();
    		for (Resource documentResource : resourceData.getRootItems()) {
    			Set<Member> documentMembers = 
    					project.getResourceMembers(documentResource.getResourceId());
    			documentMembers.forEach(
    				member -> permissionMatrix.put(documentResource, member, member.getRole()));
    			items.addItem(null, documentResource);
    			
    			Collection<Resource> collectionResources = 
    				resourceData.getChildren(documentResource);
    			for (Resource collectionResource : collectionResources) {
    				Set<Member> collectionMembers = 
    						project.getResourceMembers(collectionResource.getResourceId());
    				collectionMembers.forEach(
    					member -> permissionMatrix.put(collectionResource, member, member.getRole()));
    				items.addItem(documentResource, collectionResource);
    			}
    			
    		}

    		for (TagsetDefinition tagset : tagsets) {
    			Set<Member> tagsetMembers = project.getResourceMembers(tagset.getUuid());
    			TagsetResource tagsetResource = 
    					new TagsetResource(tagset, project.getProjectId());
    			tagsetMembers.forEach(member -> permissionMatrix.put(tagsetResource, member, member.getRole()));
    			items.addItem(null, tagsetResource);
    		}
    		
    		for (Member member : permissionMatrix.columnKeySet() ){
    			Column<Resource, String> memberColumn = permissionGrid.addColumn(resource -> {
    				RBACRole role = permissionMatrix.get(resource, member);
    				return role==null?"":role.getRoleName();
    			})
    			.setWidth(120)
    			.setCaption(member.getName())
    			.setDescriptionGenerator(resource -> member.preciseName());
    			
    			// only reporter and lower can be upgraded for specific resources
    			if (member.getRole().getAccessLevel() <= RBACRole.REPORTER.getAccessLevel()) {
    				memberColumn.setEditorBinding(createRoleEditor(member));
    			}
    			else {
    				memberColumn.setHidable(true);
    				memberColumn.setHidden(true);
    			}
    		}
    		
			permissionDataProvider = new TreeDataProvider<>(items);
			
			permissionGrid.setDataProvider(permissionDataProvider);
			permissionGrid.expand(resourceData.getRootItems());
			
		} catch (IOException e) {
			errorHandler.showAndLogError("Failed to fetch permissions", e);
		}
	}
	
	private Binding<Resource, RBACRole> createRoleEditor(Member member) {
		
		final ComboBox<RBACRole> roleBox = new ComboBox<>();
		roleBox.setEmptySelectionAllowed(false);
		Binder<Resource> binder = permissionGrid.getEditor().getBinder();
		binder.addStatusChangeListener(new StatusChangeListener() {
			
			@Override
			public void statusChange(StatusChangeEvent event) {
				Resource resource = (Resource) event.getBinder().getBean();
				Collection<RBACRole> availableRoles = new HashSet<RBACRole>();
				if (resource != null) {
					
					RBACRole role = permissionMatrix.get(resource, member);
					availableRoles.add(role);
					RBACRole projectRole = member.getRole();
					availableRoles.add(projectRole);
					
					if (projectRole.getAccessLevel() == RBACRole.REPORTER.getAccessLevel()) {
						if (resource.isCollection()) { 
							availableRoles.add(RBACRole.ASSISTANT);
						}
					}
					else if (projectRole.getAccessLevel() == RBACRole.GUEST.getAccessLevel()) {
						availableRoles.add(RBACRole.REPORTER);
						if (resource.isCollection()) {
							availableRoles.add(RBACRole.ASSISTANT);
						}
					}
				}
				roleBox.setItems(availableRoles);
				roleBox.setEnabled(availableRoles.size() > 1);
			}
		});
		return binder.bind(
				roleBox, 
				resource -> permissionMatrix.get(resource, member),
				(resource, role) -> handleRoleChange(resource, member, role));
	}


	private void handleRoleChange(Resource resource, Member member, RBACRole role) {
		if (role != null) {
			try {
				project.assignOnResource(member, role, resource.getResourceId());
				permissionMatrix.put(resource, member, role);
				permissionGrid.getEditor().cancel();
			} catch (IOException e) {
				errorHandler.showAndLogError("Error changing permissions!", e);
			}
		}
	}


	public void show() {
		UI.getCurrent().addWindow(this);
	}
}
