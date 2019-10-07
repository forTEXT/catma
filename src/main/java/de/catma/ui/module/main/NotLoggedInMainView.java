package de.catma.ui.module.main;

import com.google.common.eventbus.EventBus;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Image;
import com.vaadin.ui.Link;
import com.vaadin.ui.UI;

import de.catma.hazelcast.HazelCastService;
import de.catma.properties.CATMAPropertyKey;
import de.catma.ui.UIHelpWindow;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.LabelButton;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;
import de.catma.ui.module.main.login.AuthenticationDialog;
import de.catma.ui.module.main.signup.SignUpDialog;

/**
 * Main layout when not logged in
 * 
 * @author db
 *
 */
public class NotLoggedInMainView extends VerticalFlexLayout {

	private final UIHelpWindow uiHelpWindow = new UIHelpWindow();
	private final InitializationService initService;
	private final LoginService loginService;
	private final EventBus eventBus;
	private final HazelCastService hazelCastService;
	
	private IconButton btHelp;

	public NotLoggedInMainView(
			InitializationService initService, 
			LoginService loginService, 
			HazelCastService hazelCastService, 
			EventBus eventBus) {
		this.initService = initService;
		this.loginService = loginService;
		this.hazelCastService = hazelCastService;
		this.eventBus = eventBus;
		initComponents();
	}
	
	private void initComponents(){
		setSizeFull();
		setAlignItems(AlignItems.CENTER);
		addStyleName("home");
		

		HorizontalFlexLayout menuLayout = new HorizontalFlexLayout();
		menuLayout.setWidth("100%");
		menuLayout.setJustifyContent(JustifyContent.FLEX_END);
		menuLayout.setAlignItems(AlignItems.CENTER);
		menuLayout.addStyleName("home__menu");
		menuLayout.setWidth("100%"); //$NON-NLS-1$
		
		addComponent(menuLayout);
		
		Link aboutLink = new Link("About",
				new ExternalResource("http://www.catma.de")); //$NON-NLS-1$
		aboutLink.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(aboutLink);

		Link termsOfUseLink = new Link("Terms of use",
				new ExternalResource("http://catma.de/documentation/terms-of-use-privacy-policy/")); //$NON-NLS-1$
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

		btHelp = new IconButton(VaadinIcons.QUESTION_CIRCLE, click -> {
			if (uiHelpWindow.getParent() == null) {
				UI.getCurrent().addWindow(uiHelpWindow);
			} else {
				UI.getCurrent().removeWindow(uiHelpWindow);
			}
		});	
		menuLayout.addComponent(btHelp);


		VerticalFlexLayout contentPanel = new VerticalFlexLayout();
		contentPanel.setHeight("100%"); //$NON-NLS-1$
		contentPanel.addStyleName("home__content"); //$NON-NLS-1$
	
		ThemeResource logoResource = new ThemeResource("catma-tailright-final-cmyk.svg"); //$NON-NLS-1$	
		contentPanel.addComponent(new Image(null,logoResource));		
		
		LabelButton btn_signup = new LabelButton("Sign up", event -> new SignUpDialog("Sign Up").show());
		LabelButton btn_login = new LabelButton("Login", event -> new AuthenticationDialog(
				"Authentication",
				CATMAPropertyKey.BaseURL.getValue(
						CATMAPropertyKey.BaseURL.getDefaultValue()),
				loginService,
				initService,
				hazelCastService,
				eventBus).show());
		LabelButton btn_newsletter = new LabelButton("Newsletter");

		HorizontalFlexLayout buttonPanel = new HorizontalFlexLayout(btn_signup,btn_login,btn_newsletter);
		buttonPanel.addStyleName("home__content__btns");
		buttonPanel.setJustifyContent(JustifyContent.CENTER);
		contentPanel.addComponent(buttonPanel);
		
		addComponent(contentPanel);		
	}
	
}
