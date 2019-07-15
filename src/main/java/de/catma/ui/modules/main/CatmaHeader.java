package de.catma.ui.modules.main;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
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
public class CatmaHeader extends HorizontalLayout {

	private final EventBus eventBus;
	private final Provider<EditAccountDialog> accountDialogProvider;
	private final LoginService loginService;
	private final Label contextInformation = new Label();

	@Inject
	public CatmaHeader(Provider<EditAccountDialog> accountDialogProvider, EventBus eventBus, LoginService loginService){
        super();
        this.accountDialogProvider = accountDialogProvider;
        this.eventBus = eventBus;
        this.loginService = loginService;
        eventBus.register(this);
        initComponents();
    }


    private void initComponents() {
    	addStyleName("header");
    	setWidth("100%");
    	
		Button home = new Button("Catma " + Version.LATEST);
		home.addClickListener((evt) -> eventBus.post(new RouteToDashboardEvent()));
		home.addStyleNames(ValoTheme.BUTTON_LINK, ValoTheme.LABEL_H3);
		
        addComponent(home);
        setComponentAlignment(home, Alignment.MIDDLE_LEFT);
        
        contextInformation.addStyleName("header__context");
        addComponent(contextInformation);
        setComponentAlignment(contextInformation, Alignment.MIDDLE_CENTER);
        setExpandRatio(contextInformation, 1f);
        
        IconButton btnAccount = new IconButton( VaadinIcons.USER);
        btnAccount.setDescription(loginService.getAPI().getUser().getName());
        ContextMenu ctxAccount = new ContextMenu(btnAccount, true);
        ctxAccount.addItem("Edit Account", (item) -> {
        	EditAccountDialog editAccount = accountDialogProvider.get();
        	editAccount.show();
        });
        ctxAccount.addItem("Logout", (item) -> {
        	loginService.logout();
        	Page.getCurrent().setLocation(RepositoryPropertyKey.BaseURL.getValue());
    	});
        
        
        btnAccount.addClickListener((evt) ->  ctxAccount.open(evt.getClientX(), evt.getClientY()));
        
        addComponent(btnAccount);
        setComponentAlignment(home, Alignment.MIDDLE_RIGHT);
    }

    @Subscribe
    public void headerChangeEvent(HeaderContextChangeEvent headerContextChangeEvent){
        contextInformation.setValue(headerContextChangeEvent.getValue());
    }

}
