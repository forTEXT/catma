package de.catma.v10ui.routing;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.*;
import de.catma.v10ui.modules.main.HeaderContextChangeEvent;
import de.catma.v10ui.modules.main.MainView;
import de.catma.v10ui.modules.project.ProjectView;
import de.catma.v10ui.projects.ProjectTilesView;
import de.catma.v10ui.util.Styles;

@Route(value = Routes.PROJECT, layout=MainView.class)
@Tag("project")
public class ProjectRoute extends HtmlComponent implements HasComponents , HasUrlParameter<String>, BeforeLeaveObserver {

    private final ProjectView projectView;

    private final EventBus eventBus;

    @Inject
    public ProjectRoute(ProjectView projectView, EventBus eventBus) {
        this.projectView = projectView;
        this.eventBus = eventBus;
        addClassNames(Styles.dialog__bg);
        add(projectView);
    }

    @Override
    public void setParameter(BeforeEvent event, String projectId) {
        this.projectView.setParameter(event, projectId);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        eventBus.post(new HeaderContextChangeEvent(new Div()));
    }
}