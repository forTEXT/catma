package de.catma.ui.module.dashboard;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.vaadin.data.HasDataProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.rbac.IRBACManager;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.layout.HorizontalFlexLayout;

/**
 * Displays a list of all projects in a card style layout.
 * @author db
 */
public class ProjectListView extends VerticalLayout implements
        HasDataProvider<ProjectReference> {

    private final ProjectManager projectManager;
	private final EventBus eventBus;
	private final Comparator<ProjectReference> sortByNameAsc = (ref1,ref2) -> ref1.getName().compareTo(ref2.getName());
	private final Comparator<ProjectReference> sortByNameDesc = (ref1,ref2) -> ref2.getName().compareTo(ref1.getName());
	private final IRBACManager rbacManager;
	private final Provider<JoinProjectCard> joinProjectCardProvider;
	private Comparator<ProjectReference> selectedSortOrder = sortByNameAsc;

	@Inject
    ProjectListView(@Assisted("projectManager")ProjectManager projectManager, 
    		EventBus eventBus, 
    		IRBACManager rbacManager, 
    		Provider<JoinProjectCard> joinProjectCardProvider){ 
        this.projectManager = projectManager;
        this.eventBus = eventBus;
        this.rbacManager = rbacManager;
        this.joinProjectCardProvider = joinProjectCardProvider;
        initComponents();
        eventBus.register(this);
    }

    //data elements
    private DataProvider<ProjectReference, ?> dataProvider = DataProvider.ofItems();

    //ui elements
    private HorizontalFlexLayout projectsLayout = new HorizontalFlexLayout();

    @Override
    public void setDataProvider(final DataProvider<ProjectReference, ?> dataProvider) {
        this.dataProvider = Objects.requireNonNull(dataProvider);
        this.dataProvider.addDataProviderListener(event -> rebuild());
        rebuild();
    }

    private void rebuild() {
        projectsLayout.removeAllComponents();
        projectsLayout.addComponent(new CreateProjectCard(projectManager, eventBus));
        projectsLayout.addComponent(joinProjectCardProvider.get());
        this.dataProvider.fetch(new Query<>())
        		.sorted(selectedSortOrder)
        		.map((prj) -> new ProjectCard(prj, projectManager, eventBus, rbacManager))
                .forEach(projectsLayout::addComponent);
    }

    protected void initComponents() {
    	addStyleName("projectlist");
    	projectsLayout.addStyleNames("projectlist__list");
    	
    	setSizeFull();
    	
    	HorizontalLayout descriptionBar = new HorizontalLayout();
        Label description = new Label("All Projects");

        Label title = new Label("Title");

        IconButton sortButton = new IconButton(VaadinIcons.ARROW_DOWN);
        
        sortButton.addClickListener((evt) -> {
        	if(sortButton.getIcon().equals(VaadinIcons.ARROW_DOWN)){
        		selectedSortOrder=sortByNameDesc;
        		sortButton.setIcon(VaadinIcons.ARROW_UP);
        	}else {
        		selectedSortOrder=sortByNameAsc;
        		sortButton.setIcon(VaadinIcons.ARROW_DOWN);
        	}
        	rebuild();
        });

        descriptionBar.addComponent(description);
        descriptionBar.setExpandRatio(description, 1f);
        descriptionBar.addComponent(title);
        descriptionBar.addComponent(sortButton);
        descriptionBar.setComponentAlignment(sortButton, Alignment.MIDDLE_RIGHT);

        descriptionBar.setWidth("100%");

        addComponent(descriptionBar);

        addComponent(projectsLayout);
        setExpandRatio(projectsLayout, 1f);
    }

	@Override
	public DataProvider<ProjectReference, ?> getDataProvider() {
		return this.dataProvider;
	}

	@Override
	public void setItems(Collection<ProjectReference> items) {
		throw new RuntimeException("setItems is not implemented");
	}

	@Subscribe
	public void resourceChanged(ResourcesChangedEvent resourcesChangedEvent){
		getDataProvider().refreshAll();
	}
}
