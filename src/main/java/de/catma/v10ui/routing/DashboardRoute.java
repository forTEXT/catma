package de.catma.v10ui.routing;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import de.catma.v10ui.modules.analyze.AnalyzeView;
import de.catma.v10ui.modules.dashboard.DashboardView;
import de.catma.v10ui.modules.main.HeaderContextChangeEvent;
import de.catma.v10ui.modules.main.MainView;
import org.apache.poi.ss.formula.functions.Even;

@Route(value = Routes.DASHBOARD, layout=MainView.class)
@Tag("dashboard")
public class DashboardRoute extends HtmlComponent implements HasComponents, BeforeEnterObserver {

    private final EventBus eventBus;

    @Inject
    public DashboardRoute(DashboardView dashboardView, EventBus eventBus) {
        this.eventBus = eventBus;
        add(dashboardView);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        eventBus.post(new HeaderContextChangeEvent(new Div()));
    }
}

