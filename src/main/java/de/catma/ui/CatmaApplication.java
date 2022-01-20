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
import java.text.MessageFormat;
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
import com.google.common.eventbus.Subscribe;
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
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.repository.git.managers.GitlabManagerRestricted;
import de.catma.sqlite.SqliteService;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.di.IRemoteGitManagerFactory;
import de.catma.ui.events.RefreshEvent;
import de.catma.ui.events.TokenInvalidEvent;
import de.catma.ui.events.TokenValidEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.login.GitlabLoginService;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;
import de.catma.ui.login.Vaadin8InitializationService;
import de.catma.ui.module.main.ErrorHandler;
import de.catma.ui.module.main.signup.CreateUserDialog;
import de.catma.ui.module.main.signup.SignupTokenManager;
import de.catma.ui.util.Version;

@Theme("catma")
@PreserveOnRefresh
@Push(value=PushMode.MANUAL, transport=Transport.WEBSOCKET_XHR )
public class CatmaApplication extends UI implements KeyValueStorage,
	BackgroundServiceProvider, ErrorHandler, ParameterProvider, FocusHandler {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Map<String, String[]> parameters = new HashMap<String, String[]>();
		
    private final ConcurrentHashMap<String,Object> _attributes = new ConcurrentHashMap<String, Object>();
	private final SignupTokenManager signupTokenManager = new SignupTokenManager();
	 
	
	// Bind later when UI is ready.
	private LoginService loginservice;
	private InitializationService initService;
	private EventBus eventBus;
	private HazelCastService hazelCastService;
	private SqliteService sqliteService;
	
	@Override
	protected void init(VaadinRequest request) {
		eventBus = new EventBus();
		initService = new Vaadin8InitializationService();
		
		loginservice = new GitlabLoginService(new IRemoteGitManagerFactory() {
			
			@Override
			public IRemoteGitManagerRestricted createFromUsernameAndPassword(String username, String password)
					throws IOException {
				
				return new GitlabManagerRestricted(eventBus, initService.acquireBackgroundService(), username, password);
			}
			
			@Override
			public IRemoteGitManagerRestricted createFromImpersonationToken(String userImpersonationToken) throws IOException {
				return new GitlabManagerRestricted(eventBus, initService.acquireBackgroundService(), userImpersonationToken);
			}

		});
		
		hazelCastService = new HazelCastService();
		this.eventBus.register(this);


		try {
			sqliteService = new SqliteService();
		}
		catch (Exception e) {
			showAndLogError("error initialising sqlite service", e);
		}

		logger.info("Session: " + request.getWrappedSession().getId());
		storeParameters(request.getParameterMap());

		Page.getCurrent().setTitle("CATMA " + Version.LATEST);
		
		try {
			Component component = initService.newEntryPage(eventBus, loginservice, hazelCastService, sqliteService);
			setContent(component);
		} catch (IOException e) {
			showAndLogError("error creating landing page", e);
		}
		eventBus.post(new RouteToDashboardEvent());

		// A fresh UI and session doesn't have a request handler registered yet.
		// we need to verify tokens here too.
		
		handleRequestToken(request);
		handleRequestOauth(request);
	}
	
	@Override
	protected void refresh(VaadinRequest request) {
		super.refresh(request);
		handleRequestToken(request);
		handleRequestOauth(request);

		eventBus.post(new RefreshEvent());
	}
	
	private void storeParameters(Map<String, String[]> parameters) {
		this.parameters.putAll(parameters);
	}
	
	private void handleRequestToken(VaadinRequest request){
		if(signupTokenManager.parseUri(request.getPathInfo())) {
			SignupTokenManager tokenManager = new SignupTokenManager();
			tokenManager.handleVerify(request.getParameter("token"), eventBus);
		}
	}
	private void handleRequestOauth(VaadinRequest request){
		if(request.getParameter("code") != null 
				&& VaadinSession.getCurrent().getAttribute("OAUTHTOKEN") != null) {
			handleOauth(request);
			try {
				Component mainView = initService.newEntryPage(eventBus, loginservice, hazelCastService, sqliteService);
				UI.getCurrent().setContent(mainView);
				eventBus.post(new RouteToDashboardEvent());
				getCurrent().getPage().pushState("/catma/");
			} catch (IOException e) {
				showAndLogError("can't login properly", e);
			}
		}
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

	public String accquirePersonalTempFolder() throws IOException {
		return initService.acquirePersonalTempFolder();
	}

	public BackgroundService accuireBackgroundService() {
		return initService.acquireBackgroundService();
	}

	public <T> void submit(String caption, final ProgressCallable<T> callable, final ExecutionListener<T> listener) {
		logger.info("submitting job '" + caption + "' " + callable); //$NON-NLS-1$ //$NON-NLS-2$
		accuireBackgroundService().submit(callable, new ExecutionListener<T>() {
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
				logger.log(Level.WARNING, "couldn't cleanup UI content", e);
			}
		}
		
		loginservice.close();
		
		initService.shutdown();
		hazelCastService.stop();
		
		super.detach();
	}
	
	@Override
	public void close() {
		logger.info("Closing UI");
		getPage().setLocation(CATMAPropertyKey.LogoutURL.getValue(CATMAPropertyKey.LogoutURL.getDefaultValue()));

		super.close();
	}

	@Override
	public void showAndLogError(String message, Throwable e) {
		IRemoteGitManagerRestricted api = null;
		try {
			api = loginservice.getAPI();
		}
		catch(Exception notOfInterest) {}
		
		logger.log(Level.SEVERE, "[" + (api==null?"not logged in":api.getUsername()) + "]" + message, e); //$NON-NLS-1$ //$NON-NLS-2$

		if (message == null) {
			message = "Internal Error"; 
		}
		if (Page.getCurrent() != null) {
			HTMLNotification.show("Error", 
				MessageFormat.format(
					"An error has occurred!<br />"
					+ "The error has been logged "
					+ "but you can help us by sending an email with a more detailed description.<br />"
					+ "Or you open an issue at <a href=\"https://github.com/mpetris/catma\">GitHub</a>.<br />"
					+ "<br />The underlying error message is:<br /> {0} <br /> {1}", 
					message, e.getMessage()==null?"":e.getMessage()),
				Type.ERROR_MESSAGE);
		}
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
		return accuireBackgroundService().schedule(command, delay, unit);
	}
	
	@Override
	public Object setAttribute(String key, Object obj){
		return this._attributes.computeIfAbsent(key, (noop) -> obj);
	}

	@Override
	public Object getAttribute(String key){
		return this._attributes.get(key);
	}
	
	/**
	 * Based on: https://developers.google.com/accounts/docs/OpenIDConnect
	 * @param request
	 */
	public void handleOauth(VaadinRequest request){
		try {
			// extract answer
			String authorizationCode = request.getParameter("code"); //$NON-NLS-1$
		
			String state = request.getParameter("state"); //$NON-NLS-1$
		
			String error = request.getParameter("error"); //$NON-NLS-1$
		
			String token = (String)VaadinSession.getCurrent().getAttribute("OAUTHTOKEN");
			
			// do we have a authorization request error?
			if (error == null) {
				// no, so we validate the state token
				Totp totp = new Totp(
						CATMAPropertyKey.otpSecret.getValue()+token, 
						new Clock(Integer.valueOf(
							CATMAPropertyKey.otpDuration.getValue())));
				if (!totp.verify(state)) {
					error = "state token verification failed"; //$NON-NLS-1$
				}
			}
			
			// state token get validation success?	
			if (error == null) {
				CloseableHttpClient httpclient = HttpClients.createDefault();
				HttpPost httpPost = 
					new HttpPost(CATMAPropertyKey.Google_oauthAccessTokenRequestURL.getValue());
				List <NameValuePair> data = new ArrayList <NameValuePair>();
				data.add(new BasicNameValuePair("code", authorizationCode)); //$NON-NLS-1$
				data.add(new BasicNameValuePair("grant_type", "authorization_code")); //$NON-NLS-1$ //$NON-NLS-2$
				data.add(new BasicNameValuePair(
					"client_id", CATMAPropertyKey.Google_oauthClientId.getValue())); //$NON-NLS-1$
				data.add(new BasicNameValuePair(
					"client_secret", CATMAPropertyKey.Google_oauthClientSecret.getValue())); //$NON-NLS-1$
				data.add(new BasicNameValuePair("redirect_uri", CATMAPropertyKey.BaseURL.getValue(
						CATMAPropertyKey.BaseURL.getDefaultValue()))); //$NON-NLS-1$
				httpPost.setEntity(new UrlEncodedFormEntity(data));
				CloseableHttpResponse tokenRequestResponse = httpclient.execute(httpPost);
				HttpEntity entity = tokenRequestResponse.getEntity();
				InputStream content = entity.getContent();
				ByteArrayOutputStream bodyBuffer = new ByteArrayOutputStream();
				IOUtils.copy(content, bodyBuffer);
				
				logger.info("access token request result: " + bodyBuffer.toString("UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
				
				ObjectMapper mapper = new ObjectMapper();
		
				ObjectNode accessTokenResponseJSon = 
						mapper.readValue(bodyBuffer.toString(), ObjectNode.class);
		
				String idToken = accessTokenResponseJSon.get("id_token").asText(); //$NON-NLS-1$
				
				String[] pieces = idToken.split("\\."); //$NON-NLS-1$
				// we skip the header and go ahead with the payload
				String payload = pieces[1];
		
				String decodedPayload = 
						new String(Base64.decodeBase64(payload), "UTF-8"); //$NON-NLS-1$
				ObjectNode payloadJson = mapper.readValue(decodedPayload, ObjectNode.class);
				
				logger.info("decodedPayload: " + decodedPayload); //$NON-NLS-1$
				
				String identifier = payloadJson.get("sub").asText(); //$NON-NLS-1$
				String email = payloadJson.get("email").asText(); //$NON-NLS-1$
				String name = email.substring(0, email.indexOf("@")) + "@catma" + new Random().nextInt(); 
				String provider = "google_com";
				loginservice.loggedInFromThirdParty(identifier, provider, email, name);
				setAttribute("OAUTHTOKEN", null);
			}
			else {
		        logger.info("authentication failure: " + error); //$NON-NLS-1$
				new Notification(
		            "Authentication failure",
		            "The authentication failed!",
		            Type.ERROR_MESSAGE).show(getPage());
			}
		} catch (Exception e) {
			e.printStackTrace();
			((ErrorHandler)this).showAndLogError(
					"Error during login!", e);
		}
	}
	
	@Subscribe
	public void handleTokenValid(TokenValidEvent tokenValidEvent){
		getUI().access(() -> {
			CreateUserDialog createUserDialog = new CreateUserDialog("Create User", tokenValidEvent.getSignupToken());
			createUserDialog.show();
		});
	}
	
	@Subscribe
	public void handleTokenValid(TokenInvalidEvent tokenInvalidEvent){
		getUI().access(() -> {
			Notification.show(tokenInvalidEvent.getReason(), Type.WARNING_MESSAGE);
		});
	}
	
	public HazelCastService getHazelCastService() {
		return hazelCastService;
	}

	public SqliteService getSqliteService() {
		return sqliteService;
	}
}
