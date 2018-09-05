package de.catma.ui.project;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.v7.data.util.PropertysetItem;

import de.catma.Pager;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.dialog.SingleValueDialog;
import de.catma.ui.tabbedview.TabComponent;

public class ProjectListView extends HorizontalSplitPanel implements TabComponent {
	
	private ProjectManager projectManager;
	private Grid<ProjectReference> projectGrid;
	private Button btCreateProject;
	private Button btOpenProject;
	private ProjectListListener projectListListener;

	
	public ProjectListView(ProjectManager projectManager, ProjectListListener projectListListener) {
		this.projectManager = projectManager;
		this.projectListListener = projectListListener;
		
		initComponents();
		initActions();
		initData();
		
	}
	
	

	private void initActions() {
		btCreateProject.addClickListener(event -> {
			SingleValueDialog getNameDlg = new SingleValueDialog();
			getNameDlg.getSingleValue("Create a new Project", "Please enter the name of the Project!", new SaveCancelListener<PropertysetItem>() {
				
				@Override
				public void savePressed(PropertysetItem result) {
					try {
						String name = (String)result.getItemProperty("name").getValue();
						ProjectReference projectReference = projectManager.createProject(name, "TODO");
						//TODO:
						initData();
						projectGrid.select(projectReference);
						
//						projectGrid.getContainerDataSource().addItem(projectReference);
					}
					catch (Exception e) {
						//TODO:
						e.printStackTrace();
					}
				}
				
				@Override
				public void cancelPressed() {/*noop*/}
			}, "name");
		});
		
		btOpenProject.addClickListener(event -> {
			
			for (ProjectReference projectReference : projectGrid.getSelectedItems()) {
				openProject(projectReference);
			}
		});
	}



	private void openProject(ProjectReference projectReference) {
		projectListListener.projectOpened(projectReference);
	}



	private void initData() {
		try {
			Pager<ProjectReference> projectPager = this.projectManager.getProjectReferences();
			
			DataProvider<ProjectReference,Void> projectDataProvider = 
				DataProvider.fromCallbacks(
						query -> {
							int page = (query.getOffset() / query.getLimit())+1;
							
							return projectPager
									.page(page)
									.stream();
						},
						query -> {
							
							return projectPager.getTotalItems();
						}
				);
			projectGrid.setDataProvider(projectDataProvider);
		}
		catch (Exception e) {
			e.printStackTrace(); //TODO
		}
	}



	private void initComponents() {
		VerticalLayout leftPanel = new VerticalLayout();
		
		projectGrid = new Grid<ProjectReference>();
		projectGrid.setSizeFull();
		projectGrid.addColumn(ProjectReference::getName).setCaption("Name");
		
		HorizontalLayout buttonPanel = new HorizontalLayout();
		btCreateProject = new Button("Create Project");
		
		buttonPanel.addComponent(btCreateProject);
		
		
		btOpenProject = new Button("Open Project");
		buttonPanel.addComponent(btOpenProject);
		
		leftPanel.addComponent(projectGrid);
		leftPanel.addComponent(buttonPanel);
		
		addComponent(leftPanel);
	}



	public void addClickshortCuts() { /* noop*/	}
	
	public void removeClickshortCuts() { /* noop*/ }
	
	
}
