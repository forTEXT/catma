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
import de.catma.repository.git.managers.interfaces.RemoteGitManagerPrivileged;
import de.catma.ui.i18n.Messages;
import de.catma.ui.login.LoginService;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.user.User;

import java.io.IOException;
import java.time.LocalDate;
import java.util.stream.Collectors;

public class AccessTokenDialog extends Window {
	private final RemoteGitManagerPrivileged gitManagerPrivileged;

	private final long userId;

	private final Binder<AccessTokenData> binder = new Binder<>();
	private final AccessTokenData accessTokenData = new AccessTokenData();

	private Button btnClose;
	private Button btnCreatePersonalAccessToken;
	private VerticalLayout tokenRequestLayout;
	private HorizontalLayout tokenDisplayLayout;

	public AccessTokenDialog(RemoteGitManagerPrivileged gitManagerPrivileged, LoginService loginService) {
		this.gitManagerPrivileged = gitManagerPrivileged;

		User user = loginService.getRemoteGitManagerRestricted().getUser();
		this.userId = user.getUserId();

		setCaption(Messages.getString("AccessTokenDialog.caption"));
		initComponents();
		initActions();
	}

	private void initActions() {
		btnClose.addClickListener(evt -> close());

		btnCreatePersonalAccessToken.addClickListener(click -> {
			try {
				binder.writeBean(accessTokenData);
			}
			catch (ValidationException e) {
				Notification.show(
						Joiner.on("\n").join(
								e.getValidationErrors().stream()
										.map(ValidationResult::getErrorMessage)
										.collect(Collectors.toList())
						),
						Type.ERROR_MESSAGE
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
				tokenRequestLayout.setVisible(false);
				tokenDisplayLayout.setVisible(true);
			}
			catch (IOException e) {
				((ErrorHandler) UI.getCurrent()).showAndLogError(Messages.getString("AccessTokenDialog.tokenCreationErrorMessage"), e);
			}
		});
	}

	private void initComponents(){
		setWidth("60%");
		setModal(true);

		VerticalLayout content = new VerticalLayout();
		content.setWidthFull();

		Label lDescription = new Label(Messages.getString("AccessTokenDialog.overallDescription"), ContentMode.HTML);
		lDescription.addStyleName("label-with-word-wrap");

		tokenRequestLayout = new VerticalLayout();
		tokenRequestLayout.setWidthFull();

		TextField tfName = new TextField(Messages.getString("AccessTokenDialog.tokenName"));
		tfName.setWidth("100%");

		DateField dfDate = new DateField(Messages.getString("AccessTokenDialog.expiresAt"));
		dfDate.setWidth("100%");
		// TODO: so as not to confuse users in time zones that have a different date than the server, consider switching to ZonedDateTime
		//       and getting the current date from the browser (converting as appropriate)
		//       refs: https://vaadin.com/api/framework/8.14.3/com/vaadin/server/WebBrowser.html#getCurrentDate--
		//             https://stackoverflow.com/questions/10313668/how-to-get-the-time-of-computer-and-not-server-java-vaadin
		dfDate.setValue(LocalDate.now().plusMonths(1));

		binder.forField(tfName)
				.asRequired(Messages.getString("AccessTokenDialog.tokenNameRequired"))
				.bind(AccessTokenData::getName, AccessTokenData::setName);
		binder.forField(dfDate)
				.asRequired(Messages.getString("AccessTokenDialog.expiryDateRequired"))
				.withValidator(date -> date.compareTo(LocalDate.now()) > 0, Messages.getString("AccessTokenDialog.expiryDateMustBeInFuture"))
				.withValidator(date -> date.compareTo(LocalDate.now().plusMonths(3)) <= 0, Messages.getString("AccessTokenDialog.maxExpiryDate"))
				.bind(AccessTokenData::getExpiresAt, AccessTokenData::setExpiresAt);

		HorizontalLayout createButtonLayout = new HorizontalLayout();
		createButtonLayout.setWidth("100%");

		btnCreatePersonalAccessToken = new Button(Messages.getString("AccessTokenDialog.createPersonalAccessToken"));
		createButtonLayout.addComponent(btnCreatePersonalAccessToken);
		createButtonLayout.setComponentAlignment(btnCreatePersonalAccessToken, Alignment.MIDDLE_CENTER);

		tokenRequestLayout.addComponent(tfName);
		tokenRequestLayout.addComponent(dfDate);
		tokenRequestLayout.addComponent(createButtonLayout);

		tokenDisplayLayout = new HorizontalLayout();
		tokenDisplayLayout.setWidth("100%");
		tokenDisplayLayout.setVisible(false); // made visible when create button is clicked

		TextField tfToken = new TextField("Personal Access Token");
		tfToken.setWidth("100%");
		tfToken.setReadOnly(true);

		binder.bind(tfToken, AccessTokenData::getToken, AccessTokenData::setToken);

		JSClipboardButton jsClipboardButton = new JSClipboardButton(tfToken, VaadinIcons.CLIPBOARD_TEXT);
		jsClipboardButton.setDescription(Messages.getString("Dialog.copyToClipboard"));
		jsClipboardButton.addSuccessListener((JSClipboard.SuccessListener) () ->
				Notification.show(Messages.getString("Dialog.copyToClipboardSuccessful"))
		);

		tokenDisplayLayout.addComponent(tfToken);
		tokenDisplayLayout.addComponent(jsClipboardButton);
		tokenDisplayLayout.setComponentAlignment(tfToken, Alignment.BOTTOM_RIGHT);
		tokenDisplayLayout.setComponentAlignment(jsClipboardButton, Alignment.BOTTOM_RIGHT);
		tokenDisplayLayout.setExpandRatio(tfToken, 1f);

		HorizontalLayout dialogButtonLayout = new HorizontalLayout();
		dialogButtonLayout.setWidth("100%");

		btnClose = new Button(Messages.getString("Dialog.close"));
		dialogButtonLayout.addComponent(btnClose);
		dialogButtonLayout.setComponentAlignment(btnClose, Alignment.BOTTOM_RIGHT);

		content.addComponent(lDescription);
		content.addComponent(new Label("&nbsp;", ContentMode.HTML));
		content.addComponent(tokenRequestLayout);
		content.addComponent(new Label("&nbsp;", ContentMode.HTML));
		content.addComponent(tokenDisplayLayout);
		content.addComponent(new Label("&nbsp;", ContentMode.HTML));

		content.addComponent(dialogButtonLayout);

		setContent(content);
	}

	public void show() {
		UI.getCurrent().addWindow(this);
	}
}
