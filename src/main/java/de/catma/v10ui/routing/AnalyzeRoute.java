package de.catma.v10ui.routing;

import com.google.inject.Inject;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.Route;
import de.catma.v10ui.modules.analyze.AnalyzeView;
import de.catma.v10ui.modules.main.MainView;

@Route(value = Routes.ANALYZE, layout=MainView.class)
@Tag("analyze")
public class AnalyzeRoute extends HtmlComponent implements HasComponents {

    @Inject
    public AnalyzeRoute(AnalyzeView analyzeView) {
        add(analyzeView);
    }
}
