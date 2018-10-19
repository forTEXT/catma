package de.catma.v10ui.modules.dashboard;

import com.google.inject.Inject;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.HasDataProvider;
import com.vaadin.flow.data.binder.HasItemsAndComponents;
import com.vaadin.flow.data.provider.DataChangeEvent;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import de.catma.project.ProjectManager;
import de.catma.project.ProjectReference;
import de.catma.v10ui.components.IconButton;
import de.catma.v10ui.modules.main.ErrorLogger;

import java.util.Objects;

/**
 * Displays a list of all projects in a card style layout.
 * @author db
 */
public class ProjectList extends Composite<VerticalLayout> implements
        HasDataProvider<ProjectReference> {

    private final ProjectManager projectManager;
    private final ErrorLogger errorLogger;

    @Inject
    ProjectList(ProjectManager projectManager, ErrorLogger errorLogger) {
        this.errorLogger = errorLogger;
        this.projectManager = projectManager;
    }

    //data elements
    private DataProvider<ProjectReference, ?> dataProvider = DataProvider.ofItems();;

    //ui elements
    private final FlexLayout projectsLayout = new FlexLayout();

    @Override
    public void setDataProvider(final DataProvider<ProjectReference, ?> dataProvider) {
        this.dataProvider =Objects.requireNonNull(dataProvider);
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
        projectsLayout.removeAll();
        this.dataProvider.fetch(new Query<>()).map((prj) -> new ProjectCard(prj,projectManager,errorLogger))
                .forEach(projectsLayout::add);
    }

    @Override
    protected VerticalLayout initContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(false);
        content.setPadding(true);
        HorizontalLayout descriptionBar = new HorizontalLayout();
        Span description = new Span("all projects");
        description.setWidth("100%");

        Span title = new Span("title");

        IconButton upAction = new IconButton(VaadinIcon.ARROW_UP.create());

        descriptionBar.add(description);
        descriptionBar.add(title);
        descriptionBar.add(upAction);

        descriptionBar.setWidth("100%");
        descriptionBar.setAlignItems(FlexComponent.Alignment.BASELINE);
        descriptionBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        descriptionBar.setSpacing(true);

        content.add(descriptionBar);

        projectsLayout.addClassName("projectlist");

        content.add(projectsLayout);

        return content;
    }


}
