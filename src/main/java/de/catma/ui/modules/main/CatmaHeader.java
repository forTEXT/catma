package de.catma.ui.modules.main;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.themes.ValoTheme;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.HeaderContextChangeEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.login.LoginService;
import de.catma.ui.modules.account.EditAccountDialog;
import de.catma.ui.util.Version;


/**
 * Displays the header and context information
 *
 * @author db
 */
public class CatmaHeader extends CssLayout {

	private final EventBus eventBus;
	private final Provider<EditAccountDialog> accountDialogProvider;
	private final LoginService loginService;

	@Inject
	public CatmaHeader(Provider<EditAccountDialog> accountDialogProvider, EventBus eventBus, LoginService loginService){
        super();
        this.accountDialogProvider = accountDialogProvider;
        this.eventBus = eventBus;
        this.loginService = loginService;
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
        ctxAccount.addItem("edit account", (item) -> {
        	EditAccountDialog editAccount = accountDialogProvider.get();
        	editAccount.show();
        });
        ctxAccount.addItem("logout", (item) -> {
        	loginService.logout();
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
