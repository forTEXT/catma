package de.catma.ui.modules.main;

import javax.inject.Inject;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;

/**
 * Main entrypoint for catma, it renders a navigation and a mainSection
 *
 * @author db
 */
public class MainView extends CssLayout  { // implements RouterLayout, HasComponents, BeforeEnterObserver, AfterNavigationObserver {

    /**
     * Header part
     */

    private final CatmaHeader header;

    /**
     * mainSection is the combined section (nav and content) of catma
     */

    private final CssLayout mainSection = new CssLayout();

    /**
     * layoutSection is the content section
     */
    private final CssLayout viewSection = new CssLayout();

    /**
     * left side main navigation
     */
    private final CatmaNav navigation;

    /**
     * global communication via eventbus
     */
    private final EventBus eventBus;

    @Inject
    public MainView(EventBus eventBus) {
        this.eventBus = eventBus;
        this.header = new CatmaHeader(eventBus);
        this.navigation = new CatmaNav(eventBus);
        initComponents();
        addStyleName("main-view");
    }

    /**
     * initialize all components
     */
    private void initComponents() {
        addComponent(header);
       
        mainSection.addComponent(navigation);
        mainSection.addComponent(viewSection);
        mainSection.addStyleName("main-section");
        viewSection.addStyleName("view-section");
        addComponent(mainSection);
    }

    /**
     * render the content inside the section element. e.g. don't append to blindly to body element.
     * The implementation of adding is copied from {@link RouterLayout#showRouterLayoutContent(HasElement)}
     *
     * @param content
     */
//    @Override
//    public void showRouterLayoutContent(HasElement content) {
//        viewSection.getElement().appendChild(new Element[]{(Element)Objects.requireNonNull(content.getElement())});
//        navigation.refresh();
//    }

//    @Override
//    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
//      //  if (session == true) {
//            if(beforeEnterEvent.getLocation().getPath().equals(Routes.ROOT)) {
//                beforeEnterEvent.rerouteTo(Routes.DASHBOARD);
//            }
//      //  }
//    }

//    @Override
//    public void afterNavigation(AfterNavigationEvent event) {
//        navigation.afterNavigation(event);
//    }

    public void setContent(Component component){
    	this.viewSection.removeAllComponents();
    	this.viewSection.addComponent(component);
    }
}
