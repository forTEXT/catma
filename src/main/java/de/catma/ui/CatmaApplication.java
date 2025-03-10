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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Clock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.LogProgressListener;
import de.catma.backgroundservice.ProgressCallable;
import de.catma.hazelcast.HazelCastService;
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
import de.catma.ui.module.main.auth.AuthenticationDialog;
import de.catma.ui.util.Version;
import de.catma.user.signup.SignupTokenManager;

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
		if (request.getParameter("code") != null && VaadinSession.getCurrent().getAttribute("OAUTHTOKEN") != null) {
			// yes, handle oauth authentication result
			String[] stateParts = handleOauth(request).split(":");

			
			Component mainView = initService.newEntryPage(eventBus, loginService, hazelCastService, sqliteService);
			setContent(mainView);

			eventBus.post(new RouteToDashboardEvent());
			
			// handle 'joinproject' and 'joingroup' actions
			if (stateParts.length >= 3) {
				String action = stateParts[1];
				String actionToken = stateParts[2];
				requestTokenHandler.handleRequestToken(action, actionToken);
			}
			return true;
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
		RemoteGitManagerRestricted api = null;
		try {
			api = loginService.getAPI();
		}
		catch (Exception ignored) {}

		logger.log(
				Level.SEVERE,
				String.format("[%s] %s", api == null ? "not logged in" : api.getUsername(), message),
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

	/**
	 * based on: https://developers.google.com/identity/protocols/oauth2/openid-connect#authenticatingtheuser
	 * this starts at step 3: "Confirm the anti-forgery state token"
	 * prior steps are handled by AuthenticationDialog and its descendants
	 * 
	 * @see AuthenticationDialog#getGoogleOauthAuthorisationRequestUrl
	 */
	public String handleOauth(VaadinRequest request) {
		try {
			// extract answer
			String authorizationCode = request.getParameter("code");
			String state = request.getParameter("state");
			String error = request.getParameter("error");
			String oauthToken = (String)VaadinSession.getCurrent().getAttribute("OAUTHTOKEN");

			String decodedState = new String(Base64.decodeBase64(state), StandardCharsets.UTF_8);
			
			String[] stateParts = decodedState.split(":");
			String otpTimestamp = stateParts[0];
			
			// do we have an authorization request error?
			if (error == null) {
				// no, so we validate the oauthToken
				Totp totp = new Totp(
						CATMAPropertyKey.OTP_SECRET.getValue() + oauthToken,
						new Clock(CATMAPropertyKey.OTP_DURATION.getIntValue())
				);
				if (!totp.verify(otpTimestamp)) {
					error = "state token verification failed";
				}
			}

			// oauthToken validation success?
			if (error == null) {
				CloseableHttpClient httpclient = HttpClients.createDefault();

				HttpPost httpPost = new HttpPost(CATMAPropertyKey.GOOGLE_OAUTH_ACCESS_TOKEN_REQUEST_URL.getValue());
				List <NameValuePair> data = new ArrayList<>();
				data.add(new BasicNameValuePair("code", authorizationCode));
				data.add(new BasicNameValuePair("grant_type", "authorization_code"));
				data.add(new BasicNameValuePair("client_id", CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_ID.getValue()));
				data.add(new BasicNameValuePair("client_secret", CATMAPropertyKey.GOOGLE_OAUTH_CLIENT_SECRET.getValue()));
				// although there is no redirect happening in access token request, this parameter needs to be added as it is mandatory
				// and it needs to match the redirect_uri argument of preceding authorization code request (see AuthenticationDialog) 
				data.add(new BasicNameValuePair("redirect_uri", CATMAPropertyKey.BASE_URL.getValue()));
				httpPost.setEntity(new UrlEncodedFormEntity(data));

				CloseableHttpResponse tokenRequestResponse = httpclient.execute(httpPost);

				HttpEntity entity = tokenRequestResponse.getEntity();
				InputStream content = entity.getContent();
				ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
				IOUtils.copy(content, bodyBuffer);

				logger.info("Google OAuth access token request result: " + bodyBuffer.toString(StandardCharsets.UTF_8.name()));

				ObjectMapper mapper = new ObjectMapper();

				ObjectNode accessTokenResponseJson = mapper.readValue(bodyBuffer.toString(), ObjectNode.class);
				String idToken = accessTokenResponseJson.get("id_token").asText();
				String[] pieces = idToken.split("\\.");
				// we skip the header and go ahead with the payload
				String payload = pieces[1];
				String decodedPayload = new String(Base64.decodeBase64(payload), StandardCharsets.UTF_8);
				logger.info("Google OAuth access token request result (decoded): " + decodedPayload);

				ObjectNode payloadJson = mapper.readValue(decodedPayload, ObjectNode.class);
				String identifier = payloadJson.get("sub").asText();
				String provider = "google_com";
				String email = payloadJson.get("email").asText();
				String name = email.substring(0, email.indexOf("@")) + "@catma" + new Random().nextInt(); 

				loginService.loggedInFromThirdParty(identifier, provider, email, name);

				setAttribute("OAUTHTOKEN", null);
				
				// the decoded state might contain 'joinproject' or 'joingroup' actions which are handled up the chain
				return decodedState;
			}
			else {
				logger.warning("Google OAuth authentication failure: " + error);
				new Notification(
					"Authentication failure",
					"The authentication failed!",
					Type.ERROR_MESSAGE
				).show(getPage());
			}
		}
		catch (Exception e) {
			showAndLogError("Error during login", e);
		}
		
		return ""; // no state
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
