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

	private AccessTokenData accessTokenData = new AccessTokenData();

	private final Binder<AccessTokenData> binder = new Binder<>();
	private final IRemoteGitManagerPrivileged gitManagerPrivileged ;

	private final int userId;

	private Button btnClose;
	private Button btnCreatePersonalAccessToken;

	public AccessTokenDialog(IRemoteGitManagerPrivileged gitManagerPrivileged,
                             LoginService loginService) {
		this.gitManagerPrivileged = gitManagerPrivileged;

		User user = Objects.requireNonNull(loginService.getAPI()).getUser();
		this.userId = user.getUserId();
	
		this.setCaption("Get a Personal Access Token for GitLab");
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

			} catch (IOException e) {
				((ErrorHandler)UI.getCurrent()).showAndLogError("Couldn't create personal access token", e);
			}
		});
	}



	private void initComponents(){
		setWidth("40%");
		setHeight("80%");
		setModal(true);
		
		VerticalLayout content = new VerticalLayout();
		content.setSizeFull();
		
		Label lDescription = new Label("Information about PATs - what are they and their purpose, how they can be managed, security", ContentMode.TEXT);
		
		TextField tfName = new TextField("Name (allowing you to identify this token within GitLab later, if necessary)");
		tfName.setWidth("100%");

		DateField dfDate = new DateField("Expires at (defaults to one month from now)");
		dfDate.setWidth("100%");
		dfDate.setValue(LocalDate.now().plusMonths(1));

		binder.forField(tfName)
				.asRequired()
				.bind(AccessTokenData::getName, AccessTokenData::setName);
		binder.forField(dfDate)
				.asRequired()
				.withValidator(date -> date.compareTo(LocalDate.now()) > 0, "The expiry date must be in the future")
				.bind(AccessTokenData::getExpiresAt, AccessTokenData::setExpiresAt);

		HorizontalLayout createButtonPanel = new HorizontalLayout();
		createButtonPanel.setWidth("100%");
		
		btnCreatePersonalAccessToken = new Button("Create Personal Access Token");
		createButtonPanel.addComponent(btnCreatePersonalAccessToken);
		createButtonPanel.setComponentAlignment(btnCreatePersonalAccessToken, Alignment.MIDDLE_CENTER);

		HorizontalLayout tokenPanel = new HorizontalLayout();
		tokenPanel.setWidth("100%");

		TextField tfToken = new TextField("Personal Access Token");
		tfToken.setWidth("100%");
		tfToken.setReadOnly(true);

		binder.bind(tfToken, AccessTokenData::getToken, AccessTokenData::setToken);

		JSClipboardButton jsClipboardButton = new JSClipboardButton(tfToken, VaadinIcons.CLIPBOARD_TEXT);
		jsClipboardButton.addSuccessListener((JSClipboard.SuccessListener) () -> Notification.show("Copy to clipboard successful"));

		tokenPanel.addComponent(tfToken);
		tokenPanel.addComponent(jsClipboardButton);
		tokenPanel.setComponentAlignment(tfToken, Alignment.BOTTOM_RIGHT);
		tokenPanel.setComponentAlignment(jsClipboardButton, Alignment.BOTTOM_RIGHT);
		tokenPanel.setExpandRatio(tfToken, 1f);

		HorizontalLayout closeButtonPanel = new HorizontalLayout();
		closeButtonPanel.setWidth("100%");

		btnClose = new Button("Close");
		closeButtonPanel.addComponent(btnClose);
		closeButtonPanel.setComponentAlignment(btnClose, Alignment.BOTTOM_RIGHT);

		content.addComponent(lDescription);
		content.addComponent(new Label("&nbsp;", ContentMode.HTML));
		content.addComponent(tfName);
		content.addComponent(dfDate);
		content.addComponent(createButtonPanel);
		content.addComponent(new Label("&nbsp;", ContentMode.HTML));
		content.addComponent(tokenPanel);
		content.setExpandRatio(tokenPanel, 1f);

		content.addComponent(closeButtonPanel);

		setContent(content);

	}
	
	public void show() {
		UI.getCurrent().addWindow(this);
	}
	
}
