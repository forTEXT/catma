package de.catma.ui.module.account;

import com.google.common.base.Joiner;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.ValidationResult;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.jsclipboard.JSClipboard;
import com.vaadin.jsclipboard.JSClipboardButton;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Notification.Type;
import de.catma.repository.git.interfaces.IRemoteGitManagerPrivileged;
import de.catma.ui.login.LoginService;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.User;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Collectors;

public class AccessTokenDialog extends Window {

	private final AccessTokenData accessTokenData = new AccessTokenData();

	private final Binder<AccessTokenData> binder = new Binder<>();
	private final IRemoteGitManagerPrivileged gitManagerPrivileged ;

	private final int userId;

	private Button btnClose;
	private Button btnCreatePersonalAccessToken;
	private VerticalLayout tokenRequestPanel;
	private HorizontalLayout tokenDisplayPanel;

	public AccessTokenDialog(IRemoteGitManagerPrivileged gitManagerPrivileged,
                             LoginService loginService) {
		this.gitManagerPrivileged = gitManagerPrivileged;

		User user = Objects.requireNonNull(loginService.getAPI()).getUser();
		this.userId = user.getUserId();
	
		this.setCaption("Create a Personal Access Token");
		initComponents();
		initActions();
	}



	private void initActions() {
		btnClose.addClickListener(evt -> close());
		
		btnCreatePersonalAccessToken.addClickListener(click -> {

			try {
				binder.writeBean(accessTokenData);
			} catch (ValidationException e) {
				Notification.show(
						Joiner
						.on("\n")
						.join(e.getValidationErrors()
								.stream()
								.map(ValidationResult::getErrorMessage)
								.collect(Collectors.toList())
						)
						, Type.ERROR_MESSAGE
				);
				return;
			}

			try {
				accessTokenData.setToken(
						gitManagerPrivileged.createPersonalAccessToken(
							userId,
							accessTokenData.getName(),
							accessTokenData.getExpiresAt()
						)
				);
				binder.readBean(accessTokenData);
				tokenRequestPanel.setVisible(false);
				tokenDisplayPanel.setVisible(true);

			} catch (IOException e) {
				((ErrorHandler)UI.getCurrent()).showAndLogError("Couldn't create personal access token", e);
			}
		});
	}



	private void initComponents(){
		setWidth("60%");
		setModal(true);
		
		VerticalLayout content = new VerticalLayout();
		content.setWidthFull();
		
		Label lDescription = new Label("A Personal Access Token allows you to give an external system read-only access to your CATMA data for a limited time.<br/><br/>"
				+ "Personal Access Tokens are similar to your password, and should be handled with the same amount of care. "
				+ "<strong>Keep created tokens secret and only share them with systems that you know and trust!</strong><br/><br/>"
				+ "Some additional pointers:<br/>"
				+ "<ul><li>Give your token a name that allows you, or our support team, to easily identify it later. For example: the name of the external system it is for</li>"
				+ "<li>The token is valid for one month by default, up to a maximum of three months</li>"
				+ "<li>The token is displayed only once - if you forget to copy it, you'll have to create a new one</li>"
				+ "<li>The external system will have read-only access to <em>all</em> of your projects, as well as projects that you have joined</li>"
				+ "<li>Contact our support team if you need to revoke a token, or if you have any other query related to the use of tokens</li>"
				+ "<li>For expert users: you can also manage your tokens directly by logging in to our GitLab backend</li></ul>"
				, ContentMode.HTML
		);
		lDescription.addStyleName("label-with-word-wrap");

		tokenRequestPanel = new VerticalLayout();
		tokenRequestPanel.setWidthFull();
		
		TextField tfName = new TextField("Token Name");
		tfName.setWidth("100%");

		DateField dfDate = new DateField("Expires at");
		dfDate.setWidth("100%");
		dfDate.setValue(LocalDate.now().plusMonths(1));

		binder.forField(tfName)
				.asRequired("Token name is required")
				.bind(AccessTokenData::getName, AccessTokenData::setName);
		binder.forField(dfDate)
				.asRequired("Expiry date is required")
				.withValidator(date -> date.compareTo(LocalDate.now()) > 0, "The expiry date must be in the future")
				.withValidator(date -> date.compareTo(LocalDate.now().plusMonths(3)) <= 0, "The max. expiry date is three months from today")
				.bind(AccessTokenData::getExpiresAt, AccessTokenData::setExpiresAt);

		HorizontalLayout createButtonPanel = new HorizontalLayout();
		createButtonPanel.setWidth("100%");

		btnCreatePersonalAccessToken = new Button("Create Personal Access Token");
		createButtonPanel.addComponent(btnCreatePersonalAccessToken);
		createButtonPanel.setComponentAlignment(btnCreatePersonalAccessToken, Alignment.MIDDLE_CENTER);

		tokenRequestPanel.addComponent(tfName);
		tokenRequestPanel.addComponent(dfDate);
		tokenRequestPanel.addComponent(createButtonPanel);

		tokenDisplayPanel = new HorizontalLayout();
		tokenDisplayPanel.setWidth("100%");
		tokenDisplayPanel.setVisible(false); // made visible when create button is clicked

		TextField tfToken = new TextField("Personal Access Token");
		tfToken.setWidth("100%");
		tfToken.setReadOnly(true);

		binder.bind(tfToken, AccessTokenData::getToken, AccessTokenData::setToken);

		JSClipboardButton jsClipboardButton = new JSClipboardButton(tfToken, VaadinIcons.CLIPBOARD_TEXT);
		jsClipboardButton.setDescription("Copy to clipboard");
		jsClipboardButton.addSuccessListener((JSClipboard.SuccessListener) () -> Notification.show("Copy to clipboard successful"));

		tokenDisplayPanel.addComponent(tfToken);
		tokenDisplayPanel.addComponent(jsClipboardButton);
		tokenDisplayPanel.setComponentAlignment(tfToken, Alignment.BOTTOM_RIGHT);
		tokenDisplayPanel.setComponentAlignment(jsClipboardButton, Alignment.BOTTOM_RIGHT);
		tokenDisplayPanel.setExpandRatio(tfToken, 1f);

		HorizontalLayout dialogButtonPanel = new HorizontalLayout();
		dialogButtonPanel.setWidth("100%");

		btnClose = new Button("Close");
		dialogButtonPanel.addComponent(btnClose);
		dialogButtonPanel.setComponentAlignment(btnClose, Alignment.BOTTOM_RIGHT);

		content.addComponent(lDescription);
		content.addComponent(new Label("&nbsp;", ContentMode.HTML));
		content.addComponent(tokenRequestPanel);
		content.addComponent(new Label("&nbsp;", ContentMode.HTML));
		content.addComponent(tokenDisplayPanel);
		content.addComponent(new Label("&nbsp;", ContentMode.HTML));

		content.addComponent(dialogButtonPanel);

		setContent(content);

	}
	
	public void show() {
		UI.getCurrent().addWindow(this);
	}
	
}
