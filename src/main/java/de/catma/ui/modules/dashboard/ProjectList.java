package de.catma.ui.modules.dashboard;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.vaadin.data.HasDataProvider;
import com.vaadin.data.provider.DataChangeEvent;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;

import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.ResourcesChangedEvent;
import de.catma.ui.layout.FlexLayout;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.modules.main.ErrorHandler;

/**
 * Displays a list of all projects in a card style layout.
 * @author db
 */
public class ProjectList extends VerticalLayout implements
        HasDataProvider<ProjectReference> {

    private final ProjectManager projectManager;
    private final ErrorHandler errorLogger;
	private final EventBus eventBus;
	private final Comparator<ProjectReference> sortByNameAsc = (ref1,ref2) -> ref1.getName().compareTo(ref2.getName());
	private final Comparator<ProjectReference> sortByNameDesc = (ref1,ref2) -> ref2.getName().compareTo(ref1.getName());
	
	private Comparator<ProjectReference> selectedSortOrder = sortByNameAsc;

	@Inject
    ProjectList(ProjectManager projectManager, EventBus eventBus){ 
        this.errorLogger = (ErrorHandler)UI.getCurrent();
        this.projectManager = projectManager;
        this.eventBus = eventBus;
        initComponents();
        eventBus.register(this);
    }

    //data elements
    private DataProvider<ProjectReference, ?> dataProvider = DataProvider.ofItems();;

    //ui elements
    private HorizontalLayout projectsLayout = new HorizontalLayout();

    @Override
    public void setDataProvider(final DataProvider<ProjectReference, ?> dataProvider) {
        this.dataProvider = Objects.requireNonNull(dataProvider);
        this.dataProvider.addDataProviderListener(event -> {
            if (event instanceof DataChangeEvent.DataRefreshEvent) {
               // refresh(((DataChangeEvent.DataRefreshEvent<ProjectReference>) event).getItem());
            	rebuild();
            } else {
            	rebuild();
            }
        });
        rebuild();
    }

    private void rebuild() {
        projectsLayout.removeAllComponents();
        projectsLayout.addComponent(new CreateProjectCard(projectManager,eventBus));
        this.dataProvider.fetch(new Query<>())
        		.sorted(selectedSortOrder)
        		.map((prj) -> new ProjectCard(prj, projectManager,eventBus))
                .forEach(projectsLayout::addComponent);
    }

    protected void initComponents() {
    	addStyleName("projectlist");
    	
    	HorizontalLayout descriptionBar = new HorizontalLayout();
        Label description = new Label("All Projects");
        description.setWidth("100%");

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
        descriptionBar.addComponent(title);
        descriptionBar.addComponent(sortButton);

        descriptionBar.setWidth("100%");

        descriptionBar.setAlignItems(FlexLayout.AlignItems.BASELINE);
        descriptionBar.setJustifyContent(FlexLayout.JustifyContent.FLEX_END);

        addComponent(descriptionBar);

        projectsLayout.addStyleName("projectlist__list");
     
        addComponent(projectsLayout);
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
	public void resourceChanged(ResourcesChangedEvent<Component> resourcesChangedEvent){
		getDataProvider().refreshAll();
	}
}
