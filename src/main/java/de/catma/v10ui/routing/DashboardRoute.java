package de.catma.v10ui.routing;

import com.google.inject.Inject;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import de.catma.v10ui.modules.analyze.AnalyzeView;
import de.catma.v10ui.modules.dashboard.DashboardView;
import de.catma.v10ui.modules.main.MainView;

@Route(value = Routes.DASHBOARD, layout=MainView.class)
@Tag("dashboard")
public class DashboardRoute extends HtmlComponent implements HasComponents {

    @Inject
    public DashboardRoute(DashboardView dashboardView) {
        add(dashboardView);
    }
}

