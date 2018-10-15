package de.catma.v10ui.modules.main;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinServletConfiguration;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import de.catma.v10ui.routing.Routes;
import de.catma.v10ui.util.Version;

import java.util.Objects;

/**
 * Main entrypoint for catma, it renders a navigation and a mainSection
 *
 * @author db
 */
@Route(Routes.ROOT)
@PageTitle("Catma Main")
@HtmlImport("styles/shared-styles.html")
@Viewport(Viewport.DEFAULT)
@Theme(value = Lumo.class, variant = Lumo.LIGHT)
public class MainView extends Div implements RouterLayout, HasComponents, BeforeEnterObserver, AfterNavigationObserver {

    /**
     * Header part
     */

    private final CatmaHeader header;

    /**
     * mainSection is the combined section (nav and content) of catma
     */

    private final Section mainSection = new Section();

    /**
     * layoutSection is the content section
     */
    private final Section viewSection = new Section();

    /**
     * left side main navigation
     */
    private final CatmaNav navigation = new CatmaNav();

    /**
     * global communication via eventbus
     */
    private final EventBus eventBus;

    @Inject
    public MainView(EventBus eventBus) {
        this.eventBus = eventBus;
        this.header = new CatmaHeader(eventBus);
        initComponents();
        setClassName("main-view");
    }

    /**
     * initialize all components
     */
    private void initComponents() {
        this.add(header);
        mainSection.add(navigation);
        mainSection.add(viewSection);
        mainSection.addClassName("main-section");
        viewSection.addClassName("view-section");
        this.add(mainSection);
    }

    /**
     * render the content inside the section element. e.g. don't append to blindly to body element.
     * The implemenation of adding is copied from {@link RouterLayout#showRouterLayoutContent(HasElement)}
     *
     * @param content
     */
    @Override
    public void showRouterLayoutContent(HasElement content) {
        viewSection.getElement().appendChild(new Element[]{(Element)Objects.requireNonNull(content.getElement())});
        navigation.refresh();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
      //  if (session == true) {
            if(beforeEnterEvent.getLocation().getPath().equals(Routes.ROOT)) {
                beforeEnterEvent.rerouteTo(Routes.DASHBOARD);
            }
      //  }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        navigation.afterNavigation(event);
    }

}
