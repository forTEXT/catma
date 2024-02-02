package de.catma.ui;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.Page;
import com.vaadin.ui.Component;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.hazelcast.HazelCastService;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitlabManagerPrivileged;
import de.catma.sqlite.SqliteService;
import de.catma.ui.events.GroupsChangedEvent;
import de.catma.ui.events.ShowGroupsEvent;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.main.NotLoggedInMainView;
import de.catma.ui.module.main.auth.CreateUserDialog;
import de.catma.user.Group;
import de.catma.user.User;
import de.catma.user.signup.AccountSignupToken;
import de.catma.user.signup.GroupSignupToken;
import de.catma.user.signup.SignupTokenManager;
import de.catma.user.signup.SignupTokenManager.TokenValidityHandler;

public class RequestTokenHandler {
	
	private final SignupTokenManager signupTokenManager;
	private final EventBus eventBus;
	private final LoginService loginService;
	private final InitializationService initService;
	private final HazelCastService hazelCastService;
	private final SqliteService sqliteService;
	private final ErrorHandler errorHandler;
	private final ParameterProvider parameterProvider; 
	private final Supplier<Component> contentComponentSupplier;
	private final BackgroundServiceProvider backgroundServiceProvider;
	
	public RequestTokenHandler(SignupTokenManager signupTokenManager, EventBus eventBus, LoginService loginService,
			InitializationService initService, HazelCastService hazelCastService, SqliteService sqliteService,
			ErrorHandler errorHandler, ParameterProvider parameterProvider,
			Supplier<Component> contentComponentSupplier, BackgroundServiceProvider backgroundServiceProvider) {
		super();
		this.signupTokenManager = signupTokenManager;
		this.eventBus = eventBus;
		this.loginService = loginService;
		this.initService = initService;
		this.hazelCastService = hazelCastService;
		this.sqliteService = sqliteService;
		this.errorHandler = errorHandler;
		this.parameterProvider = parameterProvider;
		this.contentComponentSupplier = contentComponentSupplier;
		this.backgroundServiceProvider = backgroundServiceProvider;
	}

	public void handleRequestToken(String path) {
		
		switch (signupTokenManager.getTokenActionFromPath(path)) {
			case verify: {				
				// validate token to either display the reason for invalidity or show the create account creation dialog
				signupTokenManager.validateAccountSignupToken(parameterProvider.getParameter(Parameter.TOKEN), new TokenValidityHandler<AccountSignupToken>() {
					
					@Override
					public void tokenValid(AccountSignupToken signupToken) {
						CreateUserDialog createUserDialog = new CreateUserDialog(
								"Create User", signupToken.email(), 
								eventBus, loginService, initService, hazelCastService, sqliteService);
						createUserDialog.show();
					}
					
					@Override
					public void tokenInvalid(String reason) {
						// show reason for invalidity
						Notification.show(reason, Type.WARNING_MESSAGE);
					}
				});
				break;
			}
			case joingroup: {
				// validate token to either display the reason for invalidity or to join the group
				signupTokenManager.validateGroupSignupToken(parameterProvider.getParameter(Parameter.TOKEN), new TokenValidityHandler<GroupSignupToken>() {
					
					@Override
					public void tokenValid(GroupSignupToken groupSignupToken) {


						if (loginService.getAPI() != null) {
							User user = loginService.getAPI().getUser();
							try {
								
								GitlabManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
								gitlabManagerPrivileged.assignOnGroup(user, groupSignupToken.groupId());
								final UI currentUI = UI.getCurrent();
								backgroundServiceProvider.acquireBackgroundService().schedule(() -> {							
									currentUI.access(() -> {										
										try {
											eventBus.post(new GroupsChangedEvent());
											Group group = 
													loginService.getAPI().getGroups().stream().filter(g -> g.getId().equals(groupSignupToken.groupId())).findFirst().orElse(null);
											if (group != null) {
												Notification.show(
														"Info", 
														String.format("You've successfully joined the user group '%s'!", group.getName()), Type.HUMANIZED_MESSAGE);
											}
											eventBus.post(new ShowGroupsEvent());
											currentUI.push();
										} catch (Exception e2) {
											errorHandler.showAndLogError(String.format("Error loading group with ID %d!", groupSignupToken.groupId()), e2);
										}
									});
								}, 3, TimeUnit.SECONDS);
							} catch (Exception e) {
								errorHandler.showAndLogError(String.format("Error adding user %s to group with ID %d", user.getName(), groupSignupToken.groupId()), e);
							}
							finally {
								Page.getCurrent().replaceState(CATMAPropertyKey.BASE_URL.getValue());
							}
						}
						else {
							// we re-add the token to be able to use it after the login/signup
							signupTokenManager.put(groupSignupToken);
							Component contentComponent = contentComponentSupplier.get();
							if (contentComponent instanceof NotLoggedInMainView) {
								((NotLoggedInMainView)contentComponent).setSignupEmail(groupSignupToken.email());
							}
						}
					}
					
					@Override
					public void tokenInvalid(String reason) {
						// show reason for invalidity
						Notification.show(reason, Type.WARNING_MESSAGE);
					}
				});
				break;
			}
			case none: {
				break;
			}
		}
	}
}
