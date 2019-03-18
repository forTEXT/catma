package de.catma.ui.modules.main;

import com.google.common.eventbus.EventBus;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Image;
import com.vaadin.ui.Link;
import com.vaadin.ui.UI;

import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.ui.Messages;
import de.catma.ui.UIHelpWindow;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.LabelButton;
import de.catma.ui.layout.HorizontalLayout;
import de.catma.ui.layout.VerticalLayout;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;
import de.catma.ui.modules.main.login.AuthenticationDialog;
import de.catma.ui.modules.main.signup.SignUpDialog;

/**
 * Main layout when not logged in
 * 
 * @author db
 *
 */
public class NotLoggedInMainView extends VerticalLayout {

	private final UIHelpWindow uiHelpWindow = new UIHelpWindow();
	private final InitializationService initService;
	private final LoginService loginService;
	private final EventBus eventBus;
	
	private IconButton btHelp;

	public NotLoggedInMainView(InitializationService initService, LoginService loginService, EventBus eventBus) {
		this.initService = initService;
		this.loginService = loginService;
		this.eventBus = eventBus;
		initComponents();
	}
	
	private void initComponents(){
		setSizeFull();
		setAlignItems(AlignItems.CENTER);
		addStyleName("home");
		

		HorizontalLayout menuLayout = new HorizontalLayout();
		menuLayout.setWidth("100%");
		menuLayout.setJustifyContent(JustifyContent.FLEX_END);
		menuLayout.setAlignItems(AlignItems.CENTER);
		menuLayout.addStyleName("home__menu");
		menuLayout.setWidth("100%"); //$NON-NLS-1$
		
		addComponent(menuLayout);
		
		Link aboutLink = new Link(Messages.getString("CatmaApplication.about"), //$NON-NLS-1$
				new ExternalResource("http://www.catma.de")); //$NON-NLS-1$
		aboutLink.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(aboutLink);

		Link termsOfUseLink = new Link(Messages.getString("CatmaApplication.termsOfUse"), //$NON-NLS-1$
				new ExternalResource("http://www.catma.de/termsofuse")); //$NON-NLS-1$
		termsOfUseLink.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(termsOfUseLink);

		Link imprintLink = new Link("Imprint",
				new ExternalResource("http://www.catma.de/documentation/imprint"));
		imprintLink.setTargetName("_blank"); 
		menuLayout.addComponent(imprintLink);

		Link privacyLink = new Link("Privacy Statement",
				new ExternalResource("http://catma.de/documentation/privacy/")); 
		privacyLink.setTargetName("_blank");
		menuLayout.addComponent(privacyLink);
		
		Link helpLink = new Link(Messages.getString("CatmaApplication.Helpdesk"), //$NON-NLS-1$
				new ExternalResource("http://www.catma.de/helpdesk/")); //$NON-NLS-1$
		helpLink.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(helpLink);
		helpLink.setVisible(false);

		btHelp = new IconButton(VaadinIcons.QUESTION_CIRCLE, click -> {
			if (uiHelpWindow.getParent() == null) {
				UI.getCurrent().addWindow(uiHelpWindow);
			} else {
				UI.getCurrent().removeWindow(uiHelpWindow);
			}
		});	
		menuLayout.addComponent(btHelp);


		VerticalLayout contentPanel = new VerticalLayout();
		contentPanel.setHeight("100%"); //$NON-NLS-1$
		contentPanel.addStyleName("home__content"); //$NON-NLS-1$
	
		ThemeResource logoResource = new ThemeResource("catma-tailright-final-cmyk.svg"); //$NON-NLS-1$	
		contentPanel.addComponent(new Image(null,logoResource));		
		
		LabelButton btn_signup = new LabelButton("Sign up", event -> new SignUpDialog("Sign Up").show());
		LabelButton btn_login = new LabelButton("Login", event -> new AuthenticationDialog(
				Messages.getString("AuthenticationDialog.authenticateYourself"),  //$NON-NLS-1$
				RepositoryPropertyKey.BaseURL.getValue(
						RepositoryPropertyKey.BaseURL.getDefaultValue()),
				loginService,
				initService,
				eventBus).show());
		LabelButton btn_newsletter = new LabelButton("Newsletter");

		HorizontalLayout buttonPanel = new HorizontalLayout(btn_signup,btn_login,btn_newsletter);
		buttonPanel.addStyleName("home__content__btns");
		buttonPanel.setJustifyContent(JustifyContent.CENTER);
		contentPanel.addComponent(buttonPanel);
		
		addComponent(contentPanel);		
	}
	
}
