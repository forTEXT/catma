package de.catma.ui.module.project;

import java.util.Comparator;
import java.util.List;

import com.google.common.eventbus.EventBus;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectsManager;
import de.catma.project.ProjectReference;
import de.catma.ui.component.IconButton;
import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.module.dashboard.ProjectCard;

public class SelectProjectDialog extends AbstractOkCancelDialog<ProjectReference> {

	private final Comparator<ProjectReference> sortByNameAsc = 
			(ref1,ref2) -> String.CASE_INSENSITIVE_ORDER.compare(ref1.getName(), ref2.getName());
	private final Comparator<ProjectReference> sortByNameDesc = 
			(ref1,ref2) -> String.CASE_INSENSITIVE_ORDER.compare(ref1.getName(), ref2.getName())*-1;
	private Comparator<ProjectReference> selectedSortOrder = sortByNameAsc;

	private List<ProjectReference> projectRefs;
	private ProjectsManager projectManager;
	private EventBus eventBus;
	private ProjectReference result;

	public SelectProjectDialog(
			ProjectsManager projectManager, 
			EventBus eventBus,
			List<ProjectReference> projectRefs, SaveCancelListener<ProjectReference> saveCancelListener) {
		super("Select a Project", saveCancelListener);
		this.projectRefs = projectRefs;
		this.projectManager = projectManager;
		this.eventBus = eventBus;
	}

	@Override
	protected void addContent(ComponentContainer content) {
    	VerticalLayout scrollPanel = new VerticalLayout();
    	scrollPanel.setSizeFull();
    	content.addComponent(scrollPanel);
    	
    	
    	HorizontalFlexLayout projectsLayout = new HorizontalFlexLayout();
    	projectsLayout.setSizeFull();
    	
    	scrollPanel.addComponent(projectsLayout);
    	
    	projectsLayout.addStyleNames("select-project-dialog-list");
    	
    	HorizontalLayout descriptionBar = new HorizontalLayout();
        Label description = new Label("Please select the target Project:");

        Label title = new Label("Title");

        Button sortButton = new IconButton(VaadinIcons.ARROW_DOWN);
        sortButton.addClickListener(evt -> {
        	if(sortButton.getIcon().equals(VaadinIcons.ARROW_DOWN)){
        		selectedSortOrder=sortByNameDesc;
        		sortButton.setIcon(VaadinIcons.ARROW_UP);
        	}else {
        		selectedSortOrder=sortByNameAsc;
        		sortButton.setIcon(VaadinIcons.ARROW_DOWN);
        	}
        	initData(projectsLayout);
        });		

        descriptionBar.addComponent(description);
        descriptionBar.setExpandRatio(description, 1f);
        descriptionBar.addComponent(title);
        descriptionBar.addComponent(sortButton);
        descriptionBar.setComponentAlignment(sortButton, Alignment.MIDDLE_RIGHT);
        
        descriptionBar.setWidth("100%");

        content.addComponent(descriptionBar);

        content.addComponent(scrollPanel);
        ((AbstractOrderedLayout) content).setExpandRatio(scrollPanel, 1f);
    
        getBtOk().setVisible(false);
        
        initData(projectsLayout);
	
	}

	private void initData(HorizontalFlexLayout projectsLayout) {
        projectsLayout.removeAllComponents();

        projectRefs.stream()
        .sorted(selectedSortOrder)
        .map(prj -> new ProjectCard(prj, projectManager, eventBus, projectRef -> {
        	result = projectRef;
        	handleOkPressed();
        }))
        .forEach(projectsLayout::addComponent);

	}

	@Override
	protected ProjectReference getResult() {
		return this.result;
	}
	
	

}
