package de.catma.v10ui.routing;

import com.google.inject.Inject;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import de.catma.v10ui.modules.main.MainView;
import de.catma.v10ui.projects.ProjectTilesView;

@Route(value = Routes.PROJECTS, layout=MainView.class)
@Tag("project")
public class ProjectRoute extends HtmlComponent implements HasComponents {

    @Inject
    public ProjectRoute(ProjectTilesView projectTilesView) {
        add(projectTilesView);
    }

}