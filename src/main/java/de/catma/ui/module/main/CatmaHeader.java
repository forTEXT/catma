package de.catma.ui.module.main;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.contextmenu.ContextMenu;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.ui.component.IconButton;
import de.catma.ui.events.HeaderContextChangeEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.login.LoginService;
import de.catma.ui.module.account.AccessTokenDialog;
import de.catma.ui.module.account.EditAccountDialog;
import de.catma.ui.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * Displays the header and context information
 */
public class CatmaHeader extends HorizontalLayout {
	private final EventBus eventBus;
	private final LoginService loginService;
	private final RemoteGitManagerPrivileged gitManagerPrivileged;

	private Button btHome;
	private Label contextInformation;

	public CatmaHeader(EventBus eventBus, LoginService loginService, RemoteGitManagerPrivileged gitManagerPrivileged){
		super();

		this.eventBus = eventBus;
		this.loginService = loginService;
		this.gitManagerPrivileged = gitManagerPrivileged;

		initComponents();

		this.eventBus.register(this);
	}

    private void initComponents() {
    	addStyleName("header");
    	setWidth("100%");
    	
		btHome = new Button("Catma " + Version.LATEST);
		btHome.addClickListener((evt) -> eventBus.post(new RouteToDashboardEvent()));
		btHome.addStyleNames(ValoTheme.BUTTON_LINK, ValoTheme.LABEL_H3);
		btHome.addStyleName("header-home-button");
		
        addComponent(btHome);
        setComponentAlignment(btHome, Alignment.MIDDLE_LEFT);

        contextInformation = new Label("", ContentMode.HTML);
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
        ctxAccount.addItem("Sign Out", (item) -> {
        	loginService.logout();
    	});
        
        
        btnAccount.addClickListener((evt) ->  ctxAccount.open(evt.getClientX(), evt.getClientY()));
        
        addComponent(btnAccount);
        setComponentAlignment(btHome, Alignment.MIDDLE_RIGHT);
    }

    @Subscribe
    public void headerChangeEvent(HeaderContextChangeEvent headerContextChangeEvent){
    	String contextInfo = Jsoup.clean(headerContextChangeEvent.getValue(), Safelist.basic());

		if (!headerContextChangeEvent.isDashboard()) {
			contextInfo = contextInfo +
					"<span class='header-state-pill view-mode'>" +
					"<span class='Vaadin-Icons'>&#x" + Integer.toHexString(VaadinIcons.DESKTOP.getCodepoint()) + "</span>" +
					(headerContextChangeEvent.isReadonly() ? "Latest Contributions" : "Synchronized") +
					"</span>";
		}
    	
    	if (headerContextChangeEvent.isReadonly()) {
    		contextInfo = contextInfo +
					"<span class='header-state-pill rw-mode'>" +
					"<span class='Vaadin-Icons'>&#x" + Integer.toHexString(VaadinIcons.BAN.getCodepoint()) + "</span>" +
					"Read-only</span>";
    	}
    	
        contextInformation.setValue(contextInfo);
        if (headerContextChangeEvent.isDashboard()) {
        	btHome.setIcon(null);
        }
        else {
        	btHome.setIcon(VaadinIcons.HOME);
        }
    }

}
