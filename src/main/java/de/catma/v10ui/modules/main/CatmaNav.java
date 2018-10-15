package de.catma.v10ui.modules.main;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.RouterLink;
import de.catma.v10ui.routing.AnalyzeRoute;
import de.catma.v10ui.routing.ProjectRoute;
import de.catma.v10ui.routing.Routes;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Stateful Catma navigation
 *
 * It renders the the main navigation HTML elements, keep states of all destinations.
 *
 * @author db
 */
public class CatmaNav extends Nav implements AfterNavigationObserver {

    public CatmaNav(){
        refresh();
    }

    public void refresh() {
        removeAll();
        add(new H3("Project"));
        add(new H3("Analyze"));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        removeAll();
        String firstSegment = event.getLocation().getFirstSegment();
        List<String> otherSegments = event.getLocation().getSegments()
                .stream()
                .filter(seg -> !firstSegment.equals(seg))
                .collect(Collectors.toList());
        // Can't use the Router, because the public API doesn't allow to get current Route with parameters
        // query parameter still don't work correctly
        // RouteRegistry registry = UI.getCurrent().getRouter().getRegistry();
        // Optional<Class<? extends Component>> navT = registry.getNavigationTarget(firstSegment, otherSegments);

        if(firstSegment.equals(
                Routes.PROJECT) &&
                ! otherSegments.isEmpty() &&
                ! otherSegments.iterator().next().isEmpty()){
            add(new H3(new RouterLink("Project", ProjectRoute.class, otherSegments.iterator().next())));
            add(new H3(new RouterLink("Analyze", AnalyzeRoute.class)));
        } else {
            refresh();
        }

    }
}
