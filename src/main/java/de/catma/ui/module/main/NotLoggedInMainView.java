package de.catma.ui.module.main;

import com.google.common.eventbus.EventBus;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;
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
				new ExternalResource(CATMAPropertyKey.AboutURL.getValue(CATMAPropertyKey.AboutURL.getDefaultValue())));
		aboutLink.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(aboutLink);

		Link termsOfUseLink = new Link("Terms of use",
				new ExternalResource(CATMAPropertyKey.TermsOfUseURL.getValue(CATMAPropertyKey.TermsOfUseURL.getDefaultValue())));
		termsOfUseLink.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(termsOfUseLink);

		Link imprintLink = new Link("Imprint",
				new ExternalResource(CATMAPropertyKey.ImprintURL.getValue(CATMAPropertyKey.ImprintURL.getDefaultValue())));
		imprintLink.setTargetName("_blank"); 
		menuLayout.addComponent(imprintLink);

		Link privacyLink = new Link("Privacy Statement",
				new ExternalResource(CATMAPropertyKey.PrivacyStatementURL.getValue(CATMAPropertyKey.PrivacyStatementURL.getDefaultValue()))); 
		privacyLink.setTargetName("_blank");
		menuLayout.addComponent(privacyLink);

		Link statusLink = new Link("Status",
				new ExternalResource(CATMAPropertyKey.StatusURL.getValue(CATMAPropertyKey.StatusURL.getDefaultValue())));
		statusLink.setTargetName("_blank");
		statusLink.setIcon(VaadinIcons.WARNING);
		menuLayout.addComponent(statusLink);

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

		Label loginIssueTitle = new Label(
				VaadinIcons.WARNING.getHtml() + " There is currently an issue affecting non-Google sign ups and logins!",
				ContentMode.HTML
		);
		loginIssueTitle.addStyleName("title");

		Label loginIssueHelpText = new Label(
				"See the <a href=\""
						+ CATMAPropertyKey.LoginWorkaroundURL.getValue(CATMAPropertyKey.LoginWorkaroundURL.getDefaultValue())
						+ "\" target=\"_blank\">workaround for logging in</a> and the <a href=\""
						+ CATMAPropertyKey.StatusURL.getValue(CATMAPropertyKey.StatusURL.getDefaultValue())
						+ "\" target=\"_blank\">status page</a> for more information", ContentMode.HTML);

		VerticalLayout noticePanelVerticalLayout = new VerticalLayout(loginIssueTitle, loginIssueHelpText);
		noticePanelVerticalLayout.addStyleName("vlayout");

		HorizontalFlexLayout noticePanel = new HorizontalFlexLayout(noticePanelVerticalLayout);
		noticePanel.addStyleName("not-logged-in-main-view-noticepanel");
		noticePanel.setJustifyContent(JustifyContent.CENTER);
		contentPanel.addComponent(noticePanel);

		LabelButton btn_signup = new LabelButton("Sign up", event -> new SignUpDialog("Sign Up").show());
		btn_signup.setEnabled(false);

		LabelButton btn_login = new LabelButton("Sign in", event -> new AuthenticationDialog(
				"Sign in",
				CATMAPropertyKey.BaseURL.getValue(
						CATMAPropertyKey.BaseURL.getDefaultValue()),
				loginService,
				initService,
				hazelCastService,
				eventBus).show());
		Link newsLetterLink = new Link("Newsletter", new ExternalResource("https://catma.de/newsletter/"));
		newsLetterLink.setTargetName("_blank");
		newsLetterLink.addStyleName("button__label");

		HorizontalFlexLayout buttonPanel = new HorizontalFlexLayout(btn_signup,btn_login,newsLetterLink);
		buttonPanel.addStyleName("home__content__btns");
		buttonPanel.setJustifyContent(JustifyContent.CENTER);
		contentPanel.addComponent(buttonPanel);
		
		addComponent(contentPanel);		

		HorizontalFlexLayout bottomPanel = new HorizontalFlexLayout();
		bottomPanel.addStyleName("not-logged-in-main-view-fortext-bottom-panel");
		addComponent(bottomPanel);
		
		Link fortextButton = new Link("", new ExternalResource("https://fortext.net"));
		fortextButton.setIcon(new ThemeResource("fortext_logo.png"));
		fortextButton.setTargetName("_blank");
		fortextButton.addStyleName("not-logged-in-main-view-fortext-button");

		Label fortextLabel = new Label("developed and maintained</br>in cooperation with");
		fortextLabel.setContentMode(ContentMode.HTML);
		fortextLabel.addStyleName("not-logged-in-main-view-fortext-label");
		
		bottomPanel.addComponent(fortextLabel);
		bottomPanel.addComponent(fortextButton);
		
	}
	
}
