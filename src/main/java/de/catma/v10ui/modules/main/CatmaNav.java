package de.catma.v10ui.modules.main;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.router.RouterLink;
import de.catma.v10ui.routing.AnalyzeRoute;
import de.catma.v10ui.routing.ProjectRoute;

/**
 * Stateful Catma navigation
 *
 * It renders the the main navigation HTML elements, keep states of all destinations.
 *
 * @author db
 */
public class CatmaNav extends Composite<Nav> implements HasComponents {

    private final RouterLink projectsLi = new RouterLink("Projects", ProjectRoute.class);
   // private final RouterLink tagsLi = new RouterLink("Tags",Pr);
   // private final RouterLink annotateLi = new RouterLink("Annotate",null);
    private final RouterLink analyzeLi = new RouterLink("Analyze", AnalyzeRoute.class);

    private final OrderedList navigationList = new OrderedList();

    public CatmaNav(){
        initComponents();
    }

    private void initComponents() {
        navigationList.add(projectsLi);
    //    navigationList.add(tagsLi);
    //    navigationList.add(annotateLi);
        navigationList.add(analyzeLi);
        add(navigationList);
    }

}
