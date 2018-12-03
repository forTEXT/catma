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
import de.catma.v10ui.modules.tags.TagsView;
import de.catma.v10ui.util.Styles;

@Route(value = Routes.TAGS, layout=MainView.class)
@Tag("tags")
public class TagsRoute extends HtmlComponent implements HasComponents , HasUrlParameter<String>, BeforeLeaveObserver {

    private final TagsView tagsView;

    private final EventBus eventBus;

    @Inject
    public TagsRoute(TagsView tagsView, EventBus eventBus) {
        this.tagsView = tagsView;
        this.eventBus = eventBus;
        addClassNames(Styles.dialog__bg);
        add(tagsView);
    }

    @Override
    public void setParameter(BeforeEvent event, String projectId) {
        this.tagsView.setParameter(event, projectId);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        eventBus.post(new HeaderContextChangeEvent(new Div()));
    }
}