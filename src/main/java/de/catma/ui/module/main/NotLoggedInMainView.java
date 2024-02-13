package de.catma.ui.module.main;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
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
import de.catma.ui.module.main.auth.CreateUserDialog;
import de.catma.ui.module.main.auth.SignInDialog;
import de.catma.ui.module.main.auth.SignUpDialog;

import java.util.ArrayList;

public class NotLoggedInMainView extends VerticalFlexLayout {
	private final InitializationService initService;
	private final LoginService loginService;
	private final HazelCastService hazelCastService;
	private final SqliteService sqliteService;
	private final EventBus eventBus;

	private final UIHelpWindow uiHelpWindow;

	private Link statusLink;
	private VerticalLayout noticeLayoutInnerLayout;
	private String signupEmail;

	public NotLoggedInMainView(
			InitializationService initService,
			LoginService loginService,
			HazelCastService hazelCastService,
			SqliteService sqliteService,
			EventBus eventBus
	) {
		this.initService = initService;
		this.loginService = loginService;
		this.hazelCastService = hazelCastService;
		this.sqliteService = sqliteService;
		this.eventBus = eventBus;
		eventBus.register(this);

		uiHelpWindow = new UIHelpWindow();

		initComponents();
	}

	private void initComponents() {
		setSizeFull();
		setAlignItems(AlignItems.CENTER);
		addStyleName("home");

		HorizontalFlexLayout menuLayout = new HorizontalFlexLayout();
		menuLayout.setWidth("100%");
		menuLayout.setJustifyContent(JustifyContent.FLEX_END);
		menuLayout.setAlignItems(AlignItems.CENTER);
		menuLayout.addStyleName("home__menu");
		menuLayout.setWidth("100%");

		Link aboutLink = new Link("About", new ExternalResource(CATMAPropertyKey.ABOUT_URL.getValue()));
		aboutLink.setTargetName("_blank");
		menuLayout.addComponent(aboutLink);

		Link imprintLink = new Link("Imprint", new ExternalResource(CATMAPropertyKey.IMPRINT_URL.getValue()));
		imprintLink.setTargetName("_blank");
		menuLayout.addComponent(imprintLink);

		Link termsOfUseLink = new Link("Terms of Use", new ExternalResource(CATMAPropertyKey.TERMS_OF_USE_URL.getValue()));
		termsOfUseLink.setTargetName("_blank");
		menuLayout.addComponent(termsOfUseLink);

		Link privacyLink = new Link("Privacy Policy", new ExternalResource(CATMAPropertyKey.PRIVACY_POLICY_URL.getValue()));
		privacyLink.setTargetName("_blank");
		menuLayout.addComponent(privacyLink);

		statusLink = new Link("Status", new ExternalResource(CATMAPropertyKey.STATUS_URL.getValue()));
		statusLink.setTargetName("_blank");
		menuLayout.addComponent(statusLink);

		IconButton btnHelp = new IconButton(VaadinIcons.QUESTION_CIRCLE, click -> {
			if (uiHelpWindow.getParent() == null) {
				UI.getCurrent().addWindow(uiHelpWindow);
			}
			else {
				UI.getCurrent().removeWindow(uiHelpWindow);
			}
		});
		menuLayout.addComponent(btnHelp);

		addComponent(menuLayout);

		VerticalFlexLayout contentLayout = new VerticalFlexLayout();
		contentLayout.setHeight("100%");
		contentLayout.addStyleName("home__content");

		ThemeResource logoResource = new ThemeResource("img/catma-tailright-final-cmyk.svg");
		Image logoImage = new Image(null, logoResource);
		logoImage.setStyleName("not-logged-in-main-view-logo");
		contentLayout.addComponent(logoImage);

		noticeLayoutInnerLayout = new VerticalLayout();
		noticeLayoutInnerLayout.addStyleName("vlayout");

		HorizontalFlexLayout noticeLayout = new HorizontalFlexLayout(noticeLayoutInnerLayout);
		noticeLayout.addStyleName("not-logged-in-main-view-notice-layout");
		noticeLayout.setJustifyContent(JustifyContent.CENTER);
		contentLayout.addComponent(noticeLayout);

		renderNotices();

		LabelButton btnSignUp = new LabelButton("Sign Up", event -> {
			if (signupEmail != null) {				
				CreateUserDialog createUserDialog = new CreateUserDialog(
						"Create User", signupEmail, 
						eventBus, loginService, initService, hazelCastService, sqliteService);
				createUserDialog.show();
				signupEmail = null;
			}
			else {
				new SignUpDialog("Sign Up").show();
			}
		});

		LabelButton btnSignIn = new LabelButton(
				"Sign In",
				event -> new SignInDialog(
						"Sign In",
						loginService,
						initService,
						hazelCastService,
						sqliteService,
						eventBus
				).show()
		);
		Link newsLetterLink = new Link("Newsletter", new ExternalResource("https://catma.de/about/newsletter/"));
		newsLetterLink.setTargetName("_blank");
		newsLetterLink.addStyleName("button__label");

		HorizontalFlexLayout buttonLayout = new HorizontalFlexLayout(btnSignUp, btnSignIn, newsLetterLink);
		buttonLayout.addStyleName("home__content__btns");
		buttonLayout.setJustifyContent(JustifyContent.CENTER);
		contentLayout.addComponent(buttonLayout);

		addComponent(contentLayout);

		HorizontalFlexLayout fortextLayout = new HorizontalFlexLayout();
		fortextLayout.addStyleName("not-logged-in-main-view-fortext-layout");

		Link fortextButton = new Link("", new ExternalResource("https://fortext.net"));
		fortextButton.setIcon(new ThemeResource("img/fortext_logo.png"));
		fortextButton.setTargetName("_blank");
		fortextButton.addStyleName("not-logged-in-main-view-fortext-button");

		Label fortextLabel = new Label("developed and maintained</br>in cooperation with");
		fortextLabel.setContentMode(ContentMode.HTML);
		fortextLabel.addStyleName("not-logged-in-main-view-fortext-label");

		fortextLayout.addComponent(fortextLabel);
		fortextLayout.addComponent(fortextButton);

		addComponent(fortextLayout);
	}

	private void renderNotices() {
		statusLink.setIcon(null);
		noticeLayoutInnerLayout.removeAllComponents();

		ArrayList<SqliteService.SqliteModel.Notice> notices = sqliteService.getNotices();
		boolean haveIssues = notices.stream().anyMatch(notice -> notice.isIssue);

		if (!notices.isEmpty()) {
			Label noticesTitle = new Label(
					VaadinIcons.WARNING.getHtml() + " Current Notices",
					ContentMode.HTML
			);
			noticesTitle.addStyleName("title");
			noticeLayoutInnerLayout.addComponent(noticesTitle);

			notices.forEach(notice -> {
				noticeLayoutInnerLayout.addComponent(new Label(notice.message, ContentMode.HTML));
			});

			if (haveIssues) {
				statusLink.setIcon(VaadinIcons.WARNING);

				Label statusPageText = new Label(
						String.format(
								"See the <a href=\"%s\" target=\"_blank\">status page</a> for the latest information on current issues",
								CATMAPropertyKey.STATUS_URL.getValue()
						),
						ContentMode.HTML
				);
				noticeLayoutInnerLayout.addComponent(statusPageText);
			}
		}
	}

	@Subscribe
	public void handleRefresh(RefreshEvent refreshEvent) {
		renderNotices();
	}

	public void setSignupEmail(String email) {
		this.signupEmail = email;
		
	}
}
