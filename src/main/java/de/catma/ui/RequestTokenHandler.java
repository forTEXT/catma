package de.catma.ui;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.Page;
import com.vaadin.ui.Component;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.hazelcast.HazelCastService;
import de.catma.project.ProjectReference;
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
import de.catma.user.signup.ProjectSignupToken;
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
							final UI currentUI = UI.getCurrent();
							ConfirmDialog.show(
									currentUI, 
									"Join group", 
									String.format("You are logged in as '%s'. Do you want to join the user group '%s' with this account?", user, groupSignupToken.groupName()), 
									"Join", 
									"Cancel", 
									new ConfirmDialog.Listener() {
								
								@Override
								public void onClose(ConfirmDialog dialog) {
									if (dialog.isConfirmed()) {
										try {
											GitlabManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
											gitlabManagerPrivileged.assignOnGroup(user, groupSignupToken.groupId());
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
										// we re-add the token to be able to use it after the login/signup with a different account
										signupTokenManager.put(groupSignupToken);
										Page.getCurrent().replaceState(CATMAPropertyKey.BASE_URL.getValue());
										Notification.show("Info", "Please logout and re-use the link to join the group with a different account!", Type.HUMANIZED_MESSAGE);
									}
								}
							});
						}
						else {
							// we re-add the token to be able to use it after the login/signup
							signupTokenManager.put(groupSignupToken);
							Component contentComponent = contentComponentSupplier.get();
							if (contentComponent instanceof NotLoggedInMainView) {
								// use the email from the group invitation for a possible account signup since it is already verified
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
			case joinproject: {
				// validate token to either display the reason for invalidity or to join the project
				signupTokenManager.validateProjectSignupToken(parameterProvider.getParameter(Parameter.TOKEN), new TokenValidityHandler<ProjectSignupToken>() {
					
					@Override
					public void tokenValid(ProjectSignupToken projectSignupToken) {


						if (loginService.getAPI() != null) {
							User user = loginService.getAPI().getUser();
							final UI currentUI = UI.getCurrent();
							ConfirmDialog.show(
									currentUI, 
									"Join project", 
									String.format("You are logged in as '%s'. Do you want to join the project '%s' with this account?", user, projectSignupToken.projectName()), 
									"Join", 
									"Cancel", 
									new ConfirmDialog.Listener() {
								
								@Override
								public void onClose(ConfirmDialog dialog) {
									if (dialog.isConfirmed()) {
										try {
											GitlabManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
											
											gitlabManagerPrivileged.assignOnProject(
													user, 
													projectSignupToken.role(), 
													new ProjectReference(
															projectSignupToken.projectId(), 
															projectSignupToken.namespace(), 
															projectSignupToken.projectName(), 
															null)); // description is not used during role assignment
											
											backgroundServiceProvider.acquireBackgroundService().schedule(() -> {							
												currentUI.access(() -> {										
													try {
														eventBus.post(new GroupsChangedEvent());
														ProjectReference projectReference = 
																loginService.getAPI().getProjectReferences()
																.stream()
																.filter(pr -> pr.getProjectId().equals(projectSignupToken.projectId()))
																.findFirst()
																.orElse(null);
														
														if (projectReference != null) {
															Notification.show(
																	"Info", 
																	String.format("You've successfully joined the project '%s'!", projectReference.getName()), Type.HUMANIZED_MESSAGE);
														}
														currentUI.push();
													} catch (Exception e2) {
														errorHandler.showAndLogError(String.format("Error loading project with ID %d!", projectSignupToken.projectId()), e2);
													}
												});
											}, 3, TimeUnit.SECONDS);
										} catch (Exception e) {
											errorHandler.showAndLogError(String.format("Error adding user %s to project with ID %d", user.getName(), projectSignupToken.projectId()), e);
										}
										finally {
											Page.getCurrent().replaceState(CATMAPropertyKey.BASE_URL.getValue());
										}											
									}
									else {
										// we re-add the token to be able to use it after the login/signup with a different account
										signupTokenManager.put(projectSignupToken);
										Page.getCurrent().replaceState(CATMAPropertyKey.BASE_URL.getValue());
										Notification.show("Info", "Please logout and re-use the link to join the project with a different account!", Type.HUMANIZED_MESSAGE);
									}
								}
							});
						}
						else {
							// we re-add the token to be able to use it after the login/signup
							signupTokenManager.put(projectSignupToken);
							Component contentComponent = contentComponentSupplier.get();
							if (contentComponent instanceof NotLoggedInMainView) {
								// use the email from the group invitation for a possible account signup since it is already verified
								((NotLoggedInMainView)contentComponent).setSignupEmail(projectSignupToken.email());
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
				// clear everything that might have been added by oauth
				Page.getCurrent().replaceState(CATMAPropertyKey.BASE_URL.getValue());
				break;
			}
		}
	}
}
