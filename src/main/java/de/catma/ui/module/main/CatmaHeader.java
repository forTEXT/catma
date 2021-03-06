package de.catma.ui.module.main;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.HeaderContextChangeEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.login.LoginService;
import de.catma.ui.module.account.AccessTokenDialog;
import de.catma.ui.module.account.EditAccountDialog;
import de.catma.ui.util.Version;


/**
 * Displays the header and context information
 *
 * @author db
 */
public class CatmaHeader extends HorizontalLayout {

	private final EventBus eventBus;
	private final LoginService loginService;
	private final Label contextInformation = new Label();
	private Button btHome;

	public CatmaHeader(EventBus eventBus, LoginService loginService, IRemoteGitManagerPrivileged gitManagerPrivileged){
        super();
        this.eventBus = eventBus;
        this.loginService = loginService;
        eventBus.register(this);
        initComponents(gitManagerPrivileged);
    }


    private void initComponents(IRemoteGitManagerPrivileged gitManagerPrivileged) {
    	addStyleName("header");
    	setWidth("100%");
    	
		btHome = new Button("Catma " + Version.LATEST);
		btHome.addClickListener((evt) -> eventBus.post(new RouteToDashboardEvent()));
		btHome.addStyleNames(ValoTheme.BUTTON_LINK, ValoTheme.LABEL_H3);
		btHome.addStyleName("header-home-button");
		
        addComponent(btHome);
        setComponentAlignment(btHome, Alignment.MIDDLE_LEFT);
        
        contextInformation.addStyleName("header__context");
        addComponent(contextInformation);
        setComponentAlignment(contextInformation, Alignment.MIDDLE_CENTER);
        setExpandRatio(contextInformation, 1f);
        
        IconButton btnAccount = new IconButton( VaadinIcons.USER);
        btnAccount.setDescription(loginService.getAPI().getUser().getName());
        ContextMenu ctxAccount = new ContextMenu(btnAccount, true);
        ctxAccount.addItem("Edit Account", (item) -> {
        	EditAccountDialog editAccount = new EditAccountDialog(gitManagerPrivileged, loginService, eventBus);
        	editAccount.show();
        });
        ctxAccount.addItem("Get Access Token", (item) -> {
            AccessTokenDialog accessTokenDialog = new AccessTokenDialog(gitManagerPrivileged, loginService);
            accessTokenDialog.show();
        });
        ctxAccount.addItem("Logout", (item) -> {
        	loginService.logout();
    	});
        
        
        btnAccount.addClickListener((evt) ->  ctxAccount.open(evt.getClientX(), evt.getClientY()));
        
        addComponent(btnAccount);
        setComponentAlignment(btHome, Alignment.MIDDLE_RIGHT);
    }

    @Subscribe
    public void headerChangeEvent(HeaderContextChangeEvent headerContextChangeEvent){
        contextInformation.setValue(headerContextChangeEvent.getValue());
        if (headerContextChangeEvent.isDashboard()) {
        	btHome.setIcon(null);
        }
        else {
        	btHome.setIcon(VaadinIcons.HOME);
        }
    }

}
