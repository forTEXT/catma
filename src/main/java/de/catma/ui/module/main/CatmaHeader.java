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

	private Button btnHome;
	private Label lblContext;

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

		btnHome = new Button("Catma " + Version.LATEST);
		btnHome.addStyleNames(ValoTheme.BUTTON_LINK, ValoTheme.LABEL_H3, "header-home-button");
		btnHome.addClickListener((clickEvent) -> eventBus.post(new RouteToDashboardEvent()));
		addComponent(btnHome);
		setComponentAlignment(btnHome, Alignment.MIDDLE_LEFT);

		lblContext = new Label("", ContentMode.HTML);
		lblContext.addStyleName("header__context");
		addComponent(lblContext);
		setComponentAlignment(lblContext, Alignment.MIDDLE_CENTER);
		setExpandRatio(lblContext, 1f);

		IconButton btnAccount = new IconButton(VaadinIcons.USER);
		btnAccount.setDescription(loginService.getAPI().getUser().getName());

		ContextMenu accountMenu = new ContextMenu(btnAccount, true);
		accountMenu.addItem("Edit Account", (menuItem) -> {
			EditAccountDialog editAccountDialog = new EditAccountDialog(gitManagerPrivileged, loginService, eventBus);
			editAccountDialog.show();
		});
		accountMenu.addItem("Get Access Token", (menuItem) -> {
			AccessTokenDialog accessTokenDialog = new AccessTokenDialog(gitManagerPrivileged, loginService);
			accessTokenDialog.show();
		});
		accountMenu.addItem("Sign Out", (menuItem) -> loginService.logout());

		btnAccount.addClickListener((clickEvent) -> accountMenu.open(clickEvent.getClientX(), clickEvent.getClientY()));

		addComponent(btnAccount);
	}

	@Subscribe
	public void headerContextChange(HeaderContextChangeEvent headerContextChangeEvent) {
		btnHome.setIcon(headerContextChangeEvent.isDashboard() ? null : VaadinIcons.HOME);

		String contextInfo = Jsoup.clean(headerContextChangeEvent.getProjectName(), Safelist.basic());

		if (!headerContextChangeEvent.isDashboard()) {
			contextInfo = contextInfo +
					"<span class='header-state-pill view-mode'>" +
					"<span class='Vaadin-Icons'>&#x" + Integer.toHexString(VaadinIcons.DESKTOP.getCodepoint()) + "</span>" +
					(headerContextChangeEvent.isReadOnly() ? "Latest Contributions" : "Synchronized") +
					"</span>";
		}

		if (headerContextChangeEvent.isReadOnly()) {
			contextInfo = contextInfo +
					"<span class='header-state-pill rw-mode'>" +
					"<span class='Vaadin-Icons'>&#x" + Integer.toHexString(VaadinIcons.BAN.getCodepoint()) + "</span>" +
					"Read-only</span>";
		}

		lblContext.setValue(contextInfo);
	}
}
