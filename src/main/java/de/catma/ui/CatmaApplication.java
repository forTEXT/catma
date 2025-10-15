/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.ui;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.impl.client.HttpClients;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.LogProgressListener;
import de.catma.backgroundservice.ProgressCallable;
import de.catma.hazelcast.HazelCastService;
import de.catma.oauth.GoogleOauthHandler;
import de.catma.oauth.OauthIdentity;
import de.catma.properties.CATMAPropertyKey;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.repository.git.managers.interfaces.RemoteGitManagerRestricted;
import de.catma.sqlite.SqliteService;
import de.catma.ui.di.RemoteGitManagerFactory;
import de.catma.ui.dialog.ErrorDialog;
import de.catma.ui.events.RefreshEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.login.GitlabLoginService;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;
import de.catma.ui.login.Vaadin8InitializationService;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.util.Version;
import de.catma.user.signup.SignupTokenManager;
import de.catma.util.Pair;

@Theme("catma")
@PreserveOnRefresh
@Push(value=PushMode.MANUAL, transport=Transport.WEBSOCKET_XHR )
public class CatmaApplication extends UI implements KeyValueStorage, BackgroundServiceProvider, ErrorHandler, ParameterProvider, FocusHandler, RequestTokenHandlerProvider {
	private final Logger logger = Logger.getLogger(CatmaApplication.class.getName());

	private final Map<String, String[]> parameters = new HashMap<>();
	private final ConcurrentHashMap<String,Object> attributes = new ConcurrentHashMap<>();

	private final SignupTokenManager signupTokenManager = new SignupTokenManager();

	// bind later when the UI is ready
	private EventBus eventBus;
	private InitializationService initService;
	private LoginService loginService;
	private HazelCastService hazelCastService;
	private SqliteService sqliteService;
	private RequestTokenHandler requestTokenHandler;

	@Override
	protected void init(VaadinRequest request) {
		eventBus = new EventBus();
		initService = new Vaadin8InitializationService();

		loginService = new GitlabLoginService(new RemoteGitManagerFactory() {
			@Override
			public RemoteGitManagerRestricted createFromUsernameAndPassword(String username, String password) throws IOException {
				return new GitlabManagerRestricted(username, password);
			}

			@Override
			public RemoteGitManagerRestricted createFromImpersonationToken(String userImpersonationToken) throws IOException {
				return new GitlabManagerRestricted(userImpersonationToken);
			}
		});

		hazelCastService = new HazelCastService();

		try {
			sqliteService = new SqliteService();
		}
		catch (Exception e) {
			showAndLogError(null, e);
		}

		logger.info("Session: " + request.getWrappedSession().getId());
		storeParameters(request.getParameterMap());

		Page.getCurrent().setTitle("CATMA " + Version.LATEST);

		Component entryPage = initService.newEntryPage(eventBus, loginService, hazelCastService, sqliteService);
		setContent(entryPage);

		eventBus.register(this);
		eventBus.post(new RouteToDashboardEvent());

		requestTokenHandler = new RequestTokenHandler(
				signupTokenManager, eventBus, loginService, initService, 
				hazelCastService, sqliteService, 
				this, this, () -> this.getContent(), this);
		
		if (!handleRequestOauth(request)) { // handle oauth
			// otherwise handle token actions (signup, join)
			handleRequestToken();
		}
	}
	
	public void handleRequestToken() {
		requestTokenHandler.handleRequestToken(getParameter(Parameter.ACTION), getParameter(Parameter.TOKEN));		
	}

	private void storeParameters(Map<String, String[]> parameters) {
		this.parameters.putAll(parameters);
	}

	@Override
	protected void refresh(VaadinRequest request) {
		super.refresh(request);
		storeParameters(request.getParameterMap());

		if (!handleRequestOauth(request)) { // handle oauth
			// otherwise handle token actions (signup, join)
			handleRequestToken();
		}

		eventBus.post(new RefreshEvent());
	}

	private boolean handleRequestOauth(VaadinRequest request) {
		// do we have an oauth authentication process ongoing?
		if (request.getParameter("code") != null
				&& VaadinSession.getCurrent().getAttribute(GoogleOauthHandler.OAUTH_CSRF_TOKEN_SESSION_ATTRIBUTE_NAME) != null
		) {
			// yes, handle oauth authentication result
			Map<String, String> additionalStateParams = null;

			try {
				Pair<OauthIdentity, Map<String, String>> resultPair = GoogleOauthHandler.handleCallbackAndGetIdentity(
						request.getParameter("code"),
						request.getParameter("state"),
						request.getParameter("error"),
						CATMAPropertyKey.BASE_URL.getValue(),
						HttpClients.createDefault(),
						VaadinSession.getCurrent()::getAttribute,
						VaadinSession.getCurrent()::setAttribute
				);

				OauthIdentity oauthIdentity = resultPair.getFirst();
				additionalStateParams = resultPair.getSecond();

				// log the user in
				loginService.loggedInFromThirdParty(oauthIdentity.identifier(), oauthIdentity.provider(), oauthIdentity.email(), oauthIdentity.name());
			}
			catch (Exception e) {
				showAndLogError("Error during login", e);
			}

			Component mainView = initService.newEntryPage(eventBus, loginService, hazelCastService, sqliteService);
			setContent(mainView);

			eventBus.post(new RouteToDashboardEvent());

			// handle our own action and token parameters if present (for invitations - also see AuthenticationDialog.googleLinkClickListener)
			if (additionalStateParams != null && additionalStateParams.containsKey(Parameter.ACTION.getKey())
					&& additionalStateParams.containsKey(Parameter.TOKEN.getKey())
			) {
				requestTokenHandler.handleRequestToken(
						additionalStateParams.get(Parameter.ACTION.getKey()),
						additionalStateParams.get(Parameter.TOKEN.getKey())
				);
			}

			return true; // to signal that no further request token processing should occur
		}
		return false;
	}

	public Map<String, String[]> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}

	public String getParameter(Parameter parameter) {
		return getParameter(parameter.getKey());
	}

	public String getParameter(Parameter parameter, String defaultValue) {
		String value = getParameter(parameter.getKey());
		return value == null ? defaultValue : value;
	}

	public String getParameter(String key) {
		String[] values = parameters.get(key);
		if ((values != null) && (values.length > 0)) {
			return values[0];
		}
		return null;
	}

	public String[] getParameters(Parameter parameter) {
		return getParameters(parameter.getKey());
	}

	public String[] getParameters(String key) {
		return parameters.get(key);
	}

	public String acquirePersonalTempFolder() throws IOException {
		return initService.acquirePersonalTempFolder();
	}

	public BackgroundService acquireBackgroundService() {
		return initService.acquireBackgroundService();
	}

	public <T> void submit(String caption, final ProgressCallable<T> callable, final ExecutionListener<T> listener) {
		logger.info("Submitting job '" + caption + "' " + callable); //$NON-NLS-1$ //$NON-NLS-2$
		acquireBackgroundService().submit(callable, new ExecutionListener<T>() {
			public void done(T result) {
				listener.done(result);
			};

			public void error(Throwable t) {
				listener.error(t);
			}
		}, new LogProgressListener());
	}

	@Override
	public void detach() {
		logger.info("Detaching UI");
		
		Component content = getContent();
		if (content instanceof Closeable) {
			try {
				((Closeable) content).close();
			} catch (IOException e) {
				logger.log(Level.WARNING, "Couldn't clean up UI content", e);
			}
		}
		
		loginService.close();
		
		initService.shutdown();
		hazelCastService.stop();
		
		super.detach();
	}

	@Override
	public void close() {
		logger.info("Closing UI");
		getPage().setLocation(CATMAPropertyKey.LOGOUT_URL.getValue());

		super.close();
	}

	@Override
	public void showAndLogError(String message, Throwable e) {
		RemoteGitManagerRestricted remoteGitManagerRestricted = null;
		try {
			remoteGitManagerRestricted = loginService.getRemoteGitManagerRestricted();
		}
		catch (Exception ignored) {}

		logger.log(
				Level.SEVERE,
				String.format("[%s] %s", remoteGitManagerRestricted == null ? "not logged in" : remoteGitManagerRestricted.getUsername(), message),
				e
		);

		if (message == null) {
			message = "Internal Error"; 
		}

		new ErrorDialog(message, e).show();
	}

	@Override
	public void focusDeferred(Focusable focusable) {
		schedule(() -> {
			getUI().access(() -> {
				focusable.focus();
				//push();
			});
			
		}, 1, TimeUnit.SECONDS);
	}

	public ScheduledFuture<?> schedule(Runnable command,
			long delay, TimeUnit unit) {
		return acquireBackgroundService().schedule(command, delay, unit);
	}
	
	@Override
	public Object setAttribute(String key, Object obj){
		return this.attributes.computeIfAbsent(key, (noop) -> obj);
	}

	@Override
	public Object getAttribute(String key){
		return this.attributes.get(key);
	}

	public HazelCastService getHazelCastService() {
		return hazelCastService;
	}

	public SqliteService getSqliteService() {
		return sqliteService;
	}
	
	@Override
	public RequestTokenHandler getRequestTokenHandler() {
		return requestTokenHandler;
	}
}
