package de.catma.ui.modules.main;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.themes.ValoTheme;

import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.util.Version;


/**
 * Displays the header and context information
 *
 * @author db
 */
public class CatmaHeader extends CssLayout {

    private final EventBus eventBus;

	public CatmaHeader(EventBus eventBus){
        super();
        this.eventBus = eventBus;
        eventBus.register(this);
        initComponents();
    }

    private final CssLayout contextInformation = new CssLayout();

    private void initComponents() {
    	setStyleName("header");
    	
		Button home = new Button("Catma " + Version.LATEST);
		home.addClickListener((evt) -> eventBus.post(new RouteToDashboardEvent()));
		home.addStyleNames(ValoTheme.BUTTON_LINK, ValoTheme.LABEL_H3);
		
        addComponents(home);
        contextInformation.addStyleName("header__context");
        addComponents(contextInformation);

    }

    @Subscribe
    public void headerChangeEvent(HeaderContextChangeEvent headerContextChangeEvent){
        contextInformation.removeAllComponents();
        contextInformation.addComponent(headerContextChangeEvent.getValue());
    }

}
