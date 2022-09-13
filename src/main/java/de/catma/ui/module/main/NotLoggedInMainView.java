package de.catma.ui.module.main;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
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
import de.catma.sqlite.SqliteService;
import de.catma.ui.UIHelpWindow;
import de.catma.ui.component.IconButton;
import de.catma.ui.component.LabelButton;
import de.catma.ui.events.RefreshEvent;
import de.catma.ui.layout.HorizontalFlexLayout;
import de.catma.ui.layout.VerticalFlexLayout;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;
import de.catma.ui.module.main.auth.SignInDialog;
import de.catma.ui.module.main.auth.SignUpDialog;

import java.util.ArrayList;

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
	private final SqliteService sqliteService;

	private Link statusLink;
	private VerticalLayout noticePanelVerticalLayout;

	public NotLoggedInMainView(
			InitializationService initService,
			LoginService loginService,
			HazelCastService hazelCastService,
			SqliteService sqliteService,
			EventBus eventBus) {
		this.initService = initService;
		this.loginService = loginService;
		this.hazelCastService = hazelCastService;
		this.sqliteService = sqliteService;
		this.eventBus = eventBus;
		eventBus.register(this);
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
		
		Link aboutLink = new Link("About", new ExternalResource(CATMAPropertyKey.AboutURL.getValue()));
		aboutLink.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(aboutLink);

		Link imprintLink = new Link("Imprint", new ExternalResource(CATMAPropertyKey.ImprintURL.getValue()));
		imprintLink.setTargetName("_blank");
		menuLayout.addComponent(imprintLink);

		Link termsOfUseLink = new Link("Terms of Use", new ExternalResource(CATMAPropertyKey.TermsOfUseURL.getValue()));
		termsOfUseLink.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(termsOfUseLink);

		Link privacyLink = new Link("Privacy Policy", new ExternalResource(CATMAPropertyKey.PrivacyPolicyURL.getValue()));
		privacyLink.setTargetName("_blank");
		menuLayout.addComponent(privacyLink);

		statusLink = new Link("Status", new ExternalResource(CATMAPropertyKey.StatusURL.getValue()));
		statusLink.setTargetName("_blank");
		menuLayout.addComponent(statusLink);

		IconButton btHelp = new IconButton(VaadinIcons.QUESTION_CIRCLE, click -> {
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

		ThemeResource logoResource = new ThemeResource("img/catma-tailright-final-cmyk.svg"); //$NON-NLS-1$
		Image logoImage = new Image(null, logoResource);
		logoImage.setStyleName("not-logged-in-main-view-logo");
		contentPanel.addComponent(logoImage);

		noticePanelVerticalLayout = new VerticalLayout();
		noticePanelVerticalLayout.addStyleName("vlayout");

		HorizontalFlexLayout noticePanel = new HorizontalFlexLayout(noticePanelVerticalLayout);
		noticePanel.addStyleName("not-logged-in-main-view-noticepanel");
		noticePanel.setJustifyContent(JustifyContent.CENTER);
		contentPanel.addComponent(noticePanel);

		renderNotices();

		LabelButton btn_signup = new LabelButton("Sign up", event -> new SignUpDialog("Sign Up").show());

		LabelButton btn_login = new LabelButton("Sign in", event -> new SignInDialog(
				"Sign In",
				loginService,
				initService,
				hazelCastService,
				sqliteService,
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
		fortextButton.setIcon(new ThemeResource("img/fortext_logo.png"));
		fortextButton.setTargetName("_blank");
		fortextButton.addStyleName("not-logged-in-main-view-fortext-button");

		Label fortextLabel = new Label("developed and maintained</br>in cooperation with");
		fortextLabel.setContentMode(ContentMode.HTML);
		fortextLabel.addStyleName("not-logged-in-main-view-fortext-label");
		
		bottomPanel.addComponent(fortextLabel);
		bottomPanel.addComponent(fortextButton);
		
	}

	private void renderNotices() {
		statusLink.setIcon(null);
		noticePanelVerticalLayout.removeAllComponents();

		ArrayList<SqliteService.SqliteModel.Notice> notices = sqliteService.getNotices();
		boolean haveIssues = notices.stream().anyMatch(notice -> notice.isIssue);

		if (!notices.isEmpty()) {
			Label noticesTitle = new Label(
					VaadinIcons.WARNING.getHtml() + " Current Notices",
					ContentMode.HTML
			);
			noticesTitle.addStyleName("title");
			noticePanelVerticalLayout.addComponent(noticesTitle);

			notices.forEach(notice -> {
				noticePanelVerticalLayout.addComponent(new Label(notice.message, ContentMode.HTML));
			});

			if (haveIssues) {
				statusLink.setIcon(VaadinIcons.WARNING);

				Label statusPageText = new Label(
						String.format(
								"See the <a href=\"%s\" target=\"_blank\">status page</a> for the latest information on current issues",
								CATMAPropertyKey.StatusURL.getValue()
						),
						ContentMode.HTML
				);
				noticePanelVerticalLayout.addComponent(statusPageText);
			}
		}
	}

	@Subscribe
	public void handleRefresh(RefreshEvent refreshEvent) {
		renderNotices();
	}
	
}
