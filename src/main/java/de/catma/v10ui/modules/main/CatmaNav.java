package de.catma.v10ui.modules.main;

import com.google.common.collect.Lists;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.RouterLink;
import de.catma.v10ui.routing.AnalyzeRoute;
import de.catma.v10ui.routing.ProjectRoute;
import de.catma.v10ui.routing.Routes;
import org.restlet.routing.Router;

import java.util.List;

/**
 * Stateful Catma navigation
 *
 * It renders the the main navigation HTML elements, keep states of all destinations.
 *
 * @author db
 */
public class CatmaNav extends Nav {

    private final RouterLink projectsLink = new RouterLink("Project", ProjectRoute.class);
   // private final RouterLink tagsLi = new RouterLink("Tags",Pr);
   // private final RouterLink annotateLi = new RouterLink("Annotate",null);
    private final RouterLink analyzeLink = new RouterLink("Analyze", AnalyzeRoute.class);

    public CatmaNav(){
        refresh();
    }

    public void refresh() {
        removeAll();
        String firstSegment = UI.getCurrent().getInternals().getActiveViewLocation().getFirstSegment();
        if(firstSegment.equals(Routes.PROJECTS)){
            add(new H3(projectsLink));
            add(new H3(analyzeLink));
        } else {
           add(new H3("Project"));
           add(new H3("Analyze"));
        }
    }
}
