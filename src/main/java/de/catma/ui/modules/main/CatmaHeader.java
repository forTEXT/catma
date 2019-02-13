package de.catma.ui.modules.main;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.themes.ValoTheme;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.HeaderContextChangeEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.util.Version;


/**
 * Displays the header and context information
 *
 * @author db
 */
public class CatmaHeader extends CssLayout {

	private final EventBus eventBus = VaadinSession.getCurrent().getAttribute(EventBus.class);

	public CatmaHeader(){
        super();
        eventBus.register(this);
        initComponents();
    }

    private final CssLayout contextInformation = new CssLayout();

    private void initComponents() {
    	setStyleName("header");
    	
		Button home = new Button("Catma " + Version.LATEST);
		home.addClickListener((evt) -> eventBus.post(new RouteToDashboardEvent()));
		home.addStyleNames(ValoTheme.BUTTON_LINK, ValoTheme.LABEL_H3);
		
        addComponent(home);
        contextInformation.addStyleName("header__context");
        addComponent(contextInformation);
        
        IconButton btnAccount = new IconButton( VaadinIcons.USER);

        ContextMenu ctxAccount = new ContextMenu(btnAccount, true);
        ctxAccount.addItem("logout", (item) -> {
        	VaadinSession.getCurrent().close();
        	Page.getCurrent().setLocation(RepositoryPropertyKey.BaseURL.getValue());
    	});
        btnAccount.addClickListener((evt) ->  ctxAccount.open(evt.getClientX(), evt.getClientY()));
        
        addComponent(btnAccount);

    }

    @Subscribe
    public void headerChangeEvent(HeaderContextChangeEvent headerContextChangeEvent){
        contextInformation.removeAllComponents();
        contextInformation.addComponent(headerContextChangeEvent.getValue());
    }

}
