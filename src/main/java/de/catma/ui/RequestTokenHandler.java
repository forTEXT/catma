package de.catma.ui;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import de.catma.ui.events.ProjectsChangedEvent;
import de.catma.ui.events.ShowGroupsEvent;
import de.catma.ui.events.routing.RouteToProjectEvent;
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
import de.catma.user.signup.SignupTokenManager.TokenAction;
import de.catma.user.signup.SignupTokenManager.TokenValidityHandler;

public class RequestTokenHandler {
	
	private final SignupTokenManager signupTokenManager;
	private final EventBus eventBus;
	private final LoginService loginService;
	private final InitializationService initService;
	private final HazelCastService hazelCastService;
	private final SqliteService sqliteService;
	private final ErrorHandler errorHandler;
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
		this.contentComponentSupplier = contentComponentSupplier;
		this.backgroundServiceProvider = backgroundServiceProvider;
	}

	public void handleRequestToken(String action, String token) {
		
		TokenAction tokenAction = action==null?TokenAction.none:TokenAction.findAction(action);
		
		switch (tokenAction) {
			case verify: {				
				// validate token to either display the reason for invalidity or show the create account creation dialog
				signupTokenManager.validateAccountSignupToken(token, new TokenValidityHandler<AccountSignupToken>() {
					
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
						Notification tokenInvalidNotification = new Notification(reason, Type.WARNING_MESSAGE);
						tokenInvalidNotification.setDelayMsec(5000);
						tokenInvalidNotification.show(Page.getCurrent());
					}
				});
				break;
			}
			case joingroup: {
				// validate token to either display the reason for invalidity or to join the group
				signupTokenManager.validateGroupSignupToken(token, new TokenValidityHandler<GroupSignupToken>() {
					
					@Override
					public void tokenValid(GroupSignupToken groupSignupToken) {


						if (loginService.getAPI() != null) {
							try {
								final User user = loginService.getAPI().getUser();
								final UI currentUI = UI.getCurrent();
								
								if (loginService.getAPI().getGroups(true)
										.stream()
										.filter(g -> g.getId().equals(groupSignupToken.groupId()))
										.findAny()
										.isPresent()) {
									joinGroup(currentUI, user, groupSignupToken, true);
								}
								else {
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
												joinGroup(currentUI, user, groupSignupToken, false);
											}
											else {
												// we re-add the token to be able to use it after the login/signup with a different account
												signupTokenManager.put(groupSignupToken);
												Page.getCurrent().replaceState(CATMAPropertyKey.BASE_URL.getValue());
												Notification logoutNotification = new Notification(
														"Info",
														"Please logout and re-use the link to join the group with a different account!",
														Type.HUMANIZED_MESSAGE
												);
												logoutNotification.setDelayMsec(5000);
												logoutNotification.show(Page.getCurrent());
											}
										}
										
									});
								}
							}
							catch (IOException e) {
								errorHandler.showAndLogError("Error loading groups!", e);
							}
						}
						else {
							// we re-add the token to be able to use it after the login/signup
							signupTokenManager.put(groupSignupToken);
							Component contentComponent = contentComponentSupplier.get();
							if (contentComponent instanceof NotLoggedInMainView) {
								// use the email from the group invitation for a possible account signup since it is already verified
								((NotLoggedInMainView)contentComponent).setSignupEmail(groupSignupToken.email());
							}
							Notification signinNotification = new Notification(
									"Info",
									"Please sign in/up to join the group!",
									Type.HUMANIZED_MESSAGE
							);
							signinNotification.setDelayMsec(5000);
							signinNotification.show(Page.getCurrent());
						}
					}
					
					@Override
					public void tokenInvalid(String reason) {
						// show reason for invalidity
						Notification tokenInvalidNotification = new Notification(reason, Type.WARNING_MESSAGE);
						tokenInvalidNotification.setDelayMsec(5000);
						tokenInvalidNotification.show(Page.getCurrent());
						Page.getCurrent().replaceState(CATMAPropertyKey.BASE_URL.getValue());
					}
				});
				break;
			}
			case joinproject: {
				// validate token to either display the reason for invalidity or to join the project
				signupTokenManager.validateProjectSignupToken(token, new TokenValidityHandler<ProjectSignupToken>() {
					
					@Override
					public void tokenValid(ProjectSignupToken projectSignupToken) {


						if (loginService.getAPI() != null) {
							final User user = loginService.getAPI().getUser();
							final UI currentUI = UI.getCurrent();
							try {
								if (loginService.getAPI().getProjectReferences(true).stream().filter(p -> p.getProjectId().equals(projectSignupToken.projectId())).findAny().isPresent()) {
									joinProject(currentUI, user, projectSignupToken, true);
								}
								else {
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
												joinProject(currentUI, user, projectSignupToken, false);
											}
											else {
												// we re-add the token to be able to use it after the login/signup with a different account
												signupTokenManager.put(projectSignupToken);
												Page.getCurrent().replaceState(CATMAPropertyKey.BASE_URL.getValue());
												Notification logoutNotification = new Notification(
														"Info",
														"Please logout and re-use the link to join the project with a different account!",
														Type.HUMANIZED_MESSAGE
												);
												logoutNotification.setDelayMsec(5000);
												logoutNotification.show(Page.getCurrent());
											}
										}
									});
								}
							}
							catch (IOException e) {
								errorHandler.showAndLogError("Error loading projects!", e);
							}
						}
						else {
							// we re-add the token to be able to use it after the login/signup
							signupTokenManager.put(projectSignupToken);
							Component contentComponent = contentComponentSupplier.get();
							if (contentComponent instanceof NotLoggedInMainView) {
								// use the email from the group invitation for a possible account signup since it is already verified
								((NotLoggedInMainView)contentComponent).setSignupEmail(projectSignupToken.email());
							}

							Notification signinNotification = new Notification(
									"Info",
									"Please sign in/up to join the project!",
									Type.HUMANIZED_MESSAGE
							);
							signinNotification.setDelayMsec(5000);
							signinNotification.show(Page.getCurrent());
						}
					}
					
					@Override
					public void tokenInvalid(String reason) {
						// show reason for invalidity
						Notification tokenInvalidNotification = new Notification(reason, Type.WARNING_MESSAGE);
						tokenInvalidNotification.setDelayMsec(5000);
						tokenInvalidNotification.show(Page.getCurrent());
						Page.getCurrent().replaceState(CATMAPropertyKey.BASE_URL.getValue());
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
	

	private void joinGroup(final UI currentUI, final User user, final GroupSignupToken groupSignupToken, final boolean alreadyJoined) {
		try {
			GitlabManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
			gitlabManagerPrivileged.assignOnGroup(user, groupSignupToken.groupId(), groupSignupToken.expiresAt()==null?null:LocalDate.parse(groupSignupToken.expiresAt(), DateTimeFormatter.ISO_LOCAL_DATE));
			backgroundServiceProvider.acquireBackgroundService().schedule(() -> {							
				currentUI.access(() -> {										
					try {
						eventBus.post(new GroupsChangedEvent());
						eventBus.post(new ProjectsChangedEvent());
						Group group = 
								loginService.getAPI().getGroups(true).stream().filter(g -> g.getId().equals(groupSignupToken.groupId())).findFirst().orElse(null);
						if (group != null && !alreadyJoined) {
			        		new Notification(
			        				"Info", 
			        				String.format("You've successfully joined the user group '%s'!", group.getName()),
			        				Type.HUMANIZED_MESSAGE).show(currentUI.getPage());
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

	// TODO: it takes a long time to join projects or groups once the token is validated and there is no feedback to the user
	//       should the project be opened automatically at all?
	private void joinProject(final UI currentUI, final User user, final ProjectSignupToken projectSignupToken, final boolean alreadyJoined) {
		try {
			GitlabManagerPrivileged gitlabManagerPrivileged = new GitlabManagerPrivileged();
			
			gitlabManagerPrivileged.assignOnProject(
					user, 
					projectSignupToken.role(), 
					new ProjectReference(
							projectSignupToken.projectId(), 
							projectSignupToken.namespace(), 
							projectSignupToken.projectName(), 
							null),  // description is not used during role assignment
					(projectSignupToken.expiresAt()==null)?null:LocalDate.parse(projectSignupToken.expiresAt(), DateTimeFormatter.ISO_LOCAL_DATE));
			
			backgroundServiceProvider.acquireBackgroundService().schedule(() -> {							
				currentUI.access(() -> {										
					try {
						eventBus.post(new GroupsChangedEvent());
						eventBus.post(new ProjectsChangedEvent());
						
						ProjectReference projectReference = 
								loginService.getAPI().getProjectReferences(true)
								.stream()
								.filter(pr -> pr.getProjectId().equals(projectSignupToken.projectId()))
								.findFirst()
								.orElse(null);
						
						if (projectReference != null && !alreadyJoined) {
							new Notification(
									"Info", 
									String.format("You've successfully joined the project '%s'!", projectReference.getName()), 
									Type.HUMANIZED_MESSAGE).show(currentUI.getPage());
							eventBus.post(new RouteToProjectEvent(projectReference));
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
}
