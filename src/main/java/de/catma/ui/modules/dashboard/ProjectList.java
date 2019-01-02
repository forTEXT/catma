package de.catma.ui.modules.dashboard;

import java.util.Collection;
import java.util.Objects;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.HasDataProvider;
import com.vaadin.data.provider.DataChangeEvent;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.Query;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.ui.component.IconButton;
import de.catma.ui.layout.FlexLayout;
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

    ProjectList(ProjectManager projectManager, EventBus eventBus) {
        this.errorLogger = (ErrorHandler)UI.getCurrent();
        this.eventBus = eventBus;
        this.projectManager = projectManager;
        initComponents();
    }

    //data elements
    private DataProvider<ProjectReference, ?> dataProvider = DataProvider.ofItems();;

    //ui elements
    private final FlexLayout projectsLayout = new FlexLayout();

    @Override
    public void setDataProvider(final DataProvider<ProjectReference, ?> dataProvider) {
        this.dataProvider = Objects.requireNonNull(dataProvider);
        dataProvider.addDataProviderListener(event -> {
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
        this.dataProvider.fetch(new Query<>()).map((prj) -> new ProjectCard(prj, projectManager, eventBus))
                .forEach(projectsLayout::addComponent);
    }

    protected void initComponents() {
        setSpacing(false);
//        setPadding(true); TODO: add padding to CSS
        FlexLayout descriptionBar = new FlexLayout();
        Label description = new Label("all projects");
        description.setWidth("100%");

        Label title = new Label("title");

        IconButton upAction = new IconButton(VaadinIcons.ARROW_UP);

        descriptionBar.addComponent(description);
        descriptionBar.addComponent(title);
        descriptionBar.addComponent(upAction);

        descriptionBar.setWidth("100%");

        descriptionBar.setAlignItems(FlexLayout.AlignItems.BASELINE);
        descriptionBar.setJustifyContent(FlexLayout.JustifyContent.FLEX_END);
//        descriptionBar.setSpacing(true); TODO: add spacing

        addComponent(descriptionBar);

        projectsLayout.addStyleName("projectlist");

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


}
