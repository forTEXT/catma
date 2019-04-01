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
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Component;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.LogProgressListener;
import de.catma.backgroundservice.ProgressCallable;
import de.catma.document.Corpus;
import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.source.KeywordInContext;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.indexer.IndexedRepository;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.queryengine.result.computation.DistributionSelectionListener;
import de.catma.repository.git.interfaces.IRemoteGitManagerRestricted;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.analyzer.AnalyzerProvider;
import de.catma.ui.analyzer.QueryOptionsProvider;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.events.RegisterCloseableEvent;
import de.catma.ui.events.TokenInvalidEvent;
import de.catma.ui.events.TokenValidEvent;
import de.catma.ui.events.routing.RouteToDashboardEvent;
import de.catma.ui.login.InitializationService;
import de.catma.ui.login.LoginService;
import de.catma.ui.modules.main.ErrorHandler;
import de.catma.ui.modules.main.signup.CreateUserDialog;
import de.catma.ui.modules.main.signup.SignupTokenManager;
import de.catma.ui.repository.Messages;
import de.catma.ui.tagger.TaggerView;
import de.catma.ui.tagmanager.TagsetSelectionListener;
import de.catma.ui.util.Version;

@Theme("catma")
@PreserveOnRefresh
@Push(value=PushMode.MANUAL, transport=Transport.WEBSOCKET_XHR )
public class CatmaApplication extends UI implements KeyValueStorage,
	BackgroundServiceProvider, ErrorHandler, AnalyzerProvider, ParameterProvider, FocusHandler {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Map<String, String[]> parameters = new HashMap<String, String[]>();
		
	private final List<WeakReference<Closeable>> closeListener = Lists.newArrayList();
	
    private final ConcurrentHashMap<String,Object> _attributes = new ConcurrentHashMap<String, Object>();
	private final SignupTokenManager signupTokenManager = new SignupTokenManager();

	private  Class<? extends Annotation> loginType; 
	private  Class<? extends Annotation> initType;
	 
	private LoginService loginservice;
	private InitializationService initService;

	private final Injector injector;
	private  EventBus eventBus;
	
	@Inject
	public CatmaApplication(Injector injector) throws IOException {
		this.injector = injector;
	}


	@Override
	protected void init(VaadinRequest request) {
		this.eventBus = injector.getInstance(EventBus.class);
		this.eventBus.register(this);
		try {
			
			loginType = 
					(Class<? extends Annotation>)Class.forName(RepositoryPropertyKey.LoginType.getValue());
			initType = 
					(Class<? extends Annotation>)Class.forName(RepositoryPropertyKey.InitType.getValue());
			loginservice = injector.getInstance(Key.get(LoginService.class, loginType));
			initService = injector.getInstance(Key.get(InitializationService.class, initType));
		} catch (ClassNotFoundException e) {
			showAndLogError("Runtime configuration error", e);
		}

		loginservice = injector.getInstance(Key.get(LoginService.class, loginType));
		initService = injector.getInstance(Key.get(InitializationService.class, initType));

		logger.info("Session: " + request.getWrappedSession().getId());
		storeParameters(request.getParameterMap());
		
		boolean nase = false;
		//TODO: remove test code
		
		if (nase) {
			VerticalLayout nasenPanel = new VerticalLayout();
			nasenPanel.addComponent(new Label("Nase1"));
			nasenPanel.addComponent(new Label("Nase2"));
			nasenPanel.addComponent(new Label("Nase3"));
			
			
			TreeData<String> treeData = new TreeData<>();
			for (int i=0; i<8;i++) {
				treeData.addItem(null, "item"+i);
			}
			
			for (int i=0; i<10; i++) {
				treeData.addItem("item5", "child"+i);
			}
			TreeDataProvider<String> dp = new TreeDataProvider<>(treeData);
			TreeGrid<String> grid = new TreeGrid<String>(dp);
			grid.setHierarchyColumn(
					grid.addColumn(item -> item.toString()).setCaption("Name"));
			ButtonRenderer<String> buttonRenderer = new ButtonRenderer<>(clickEvent -> System.out.println("clicked"));
			buttonRenderer.setHtmlContentAllowed(true);
			grid.addColumn(item->VaadinIcons.ABACUS.getHtml(), buttonRenderer).setCaption("Click").setHidable(true);
			nasenPanel.addComponent(grid);
			
			setContent(nasenPanel);
			return;
		}

		Page.getCurrent().setTitle(Version.LATEST.toString()); //$NON-NLS-1$
		
		try {
			Component component = initService.newEntryPage(loginservice);
			setContent(component);


	        // implement a custom resize propagation for all Layouts including CSSLayouts
	        JavaScript.getCurrent().addFunction("browserWindowResized", e -> {
	        	this.markAsDirtyRecursive();
	        });
	        Page.getCurrent().getJavaScript().execute(
	        		"var timeout = null;"
	        				+ "window.onresize = function() { "
	        				+ "  if (timeout != null) clearTimeout(timeout); "
	        				+ "  timeout = setTimeout(function() {"
	        				+ "    browserWindowResized(); "
	        				+ "  }, 250);"
	        				+ "}");
	                

		} catch (IOException e) {
			showAndLogError("error creating landing page",e);			
		}
		eventBus.post(new RouteToDashboardEvent());

		// A fresh UI and session doesn't have a request handler registered yet.
		// we need to verify tokens here too.
		
		if(signupTokenManager.parseUri(request.getPathInfo())) {
			SignupTokenManager tokenManager = new SignupTokenManager();
			tokenManager.handleVerify( request.getParameter("token"), eventBus);
		}
		
//		this.setPollInterval(1000);
	}
	
	@Override
	protected void refresh(VaadinRequest request) {
		super.refresh(request);
		if(signupTokenManager.parseUri(request.getPathInfo())) {
			SignupTokenManager tokenManager = new SignupTokenManager();
			tokenManager.handleVerify( request.getParameter("token"), eventBus);
		}
		if(request.getParameter("code") != null && VaadinSession.getCurrent().getAttribute("OAUTHTOKEN") != null) {
			handleOauth(request);
			Component mainView;
			try {
				mainView = initService.newEntryPage(loginservice);
				UI.getCurrent().setContent(mainView);
				eventBus.post(new RouteToDashboardEvent());
				getCurrent().getPage().pushState("/");
			} catch (IOException e) {
				showAndLogError("can't login properly", e);
			}
		}
	}
	
	private void storeParameters(Map<String, String[]> parameters) {
		this.parameters.putAll(parameters);
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

	
	@Deprecated
	public void addTagsetToActiveDocument(TagsetDefinition tagsetDefinition, TagsetSelectionListener tagsetSelectionListener) {
		
		//TODO: not needed anymore
	}
	
	public void addTagsetToActiveDocument(TagsetDefinition tagsetDefinition) {
		addTagsetToActiveDocument(tagsetDefinition, null);
	}

	public void openTagLibrary(Repository repository, TagLibrary tagLibrary) {
		openTagLibrary(repository, tagLibrary, true);
	}

	@Deprecated
	public void openTagLibrary(Repository repository, TagLibrary tagLibrary, boolean switchToTagManagerView) {
	}

	public TaggerView openSourceDocument(String sourceDocumentId) {
		//TODO:
//		RepositoryManager repositoryManager = repositoryManagerView.getRepositoryManager();
//
//		if (repositoryManager.hasOpenRepository()) {
//			Repository repository = repositoryManager.getFirstOpenRepository();
//
//			SourceDocument sourceDocument = repository.getSourceDocument(sourceDocumentId);
//			if (sourceDocument != null) {
//				return openSourceDocument(sourceDocument, repository);
//			}
//		}

		return null;
	}

	@Deprecated
	public TaggerView openSourceDocument(SourceDocument sourceDocument, Repository repository) {
		return null;
	}

	public String accquirePersonalTempFolder() throws IOException {
		return initService.accquirePersonalTempFolder();
	}

	public BackgroundService accuireBackgroundService() {
		return initService.accuireBackgroundService();
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

	@Deprecated
	public void openUserMarkupCollection(SourceDocument sourceDocument, UserMarkupCollection userMarkupCollection,
			Repository repository) {
		//projectManagerView.openUserMarkupCollection(sourceDocument, userMarkupCollection, repository);
	}

	@Deprecated
	public void analyze(Corpus corpus, IndexedRepository repository) {
//		projectManagerView.analyze(corpus, repository);
	}

	public void analyzeCurrentlyActiveDocument() {
		//TODO:
//		projectManagerView.analyzeCurrentlyActiveDocument(repository);
	}
	
	

	public int addVisualization(Integer visualizationId, String caption,
			DistributionComputation distributionComputation,
			DistributionSelectionListener distributionSelectionListener) {

		//TODO:
//		menu.executeEntry(visualizationManagerView);
//
//		return visualizationManagerView.addVisualization(visualizationId, caption, distributionComputation,
//				distributionSelectionListener);
		
		return 0;
	}

	@Override
	public void close() {

		for (WeakReference<Closeable> weakReference : closeListener) {
			Closeable ref = weakReference.get();
			if (ref != null) {
				try {
					ref.close();
				} catch (IOException e) {
					logger.log(Level.INFO,"couldn't cleanup resource",e);
				}
			}
		}
		
		IRemoteGitManagerRestricted api = loginservice.getAPI();
		
		if(api != null){
			logger.info("application for user " + api.getUsername() + " has been closed");
		}
		
		initService.shutdown();
		super.close();
	}

	@Override
	public void showAndLogError(String message, Throwable e) {
		IRemoteGitManagerRestricted api = loginservice.getAPI();
		
		if(api != null){
			logger.log(Level.SEVERE, "[" + api.getUsername() + "]" + message, e); //$NON-NLS-1$ //$NON-NLS-2$
			
		}

		if (message == null) {
			message = Messages.getString("CatmaApplication.internalError"); //$NON-NLS-1$
		}
		if (Page.getCurrent() != null) {
			HTMLNotification.show(Messages.getString("CatmaApplication.error"), //$NON-NLS-1$
					MessageFormat.format(Messages.getString("CatmaApplication.errorOccurred"), message, e.getMessage()), //$NON-NLS-1$
					Type.ERROR_MESSAGE);
		}
	}

	public void openSourceDocument(SourceDocument sd, Repository repository, Range range) {
		TaggerView tv = openSourceDocument(sd, repository);
		tv.show(range);
	}

	public void addDoubleTree(List<KeywordInContext> kwics) {
		//TODO:
//		menu.executeEntry(visualizationManagerView);
//		visualizationManagerView.addDoubleTree(kwics);
	}

	public void addVega(QueryResult queryResult, QueryOptionsProvider queryOptionsProvider) {
		//TODO:
//		menu.executeEntry(visualizationManagerView);
//		visualizationManagerView.addVega(queryResult, queryOptionsProvider);
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
					RepositoryPropertyKey.otpSecret.getValue()+token, 
					new Clock(Integer.valueOf(
						RepositoryPropertyKey.otpDuration.getValue())));
			if (!totp.verify(state)) {
				error = "state token verification failed"; //$NON-NLS-1$
			}
		}
		
		// state token get validation success?	
		if (error == null) {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpPost httpPost = 
				new HttpPost(RepositoryPropertyKey.Google_oauthAccessTokenRequestURL.getValue());
			List <NameValuePair> data = new ArrayList <NameValuePair>();
			data.add(new BasicNameValuePair("code", authorizationCode)); //$NON-NLS-1$
			data.add(new BasicNameValuePair("grant_type", "authorization_code")); //$NON-NLS-1$ //$NON-NLS-2$
			data.add(new BasicNameValuePair(
				"client_id", RepositoryPropertyKey.Google_oauthClientId.getValue())); //$NON-NLS-1$
			data.add(new BasicNameValuePair(
				"client_secret", RepositoryPropertyKey.Google_oauthClientSecret.getValue())); //$NON-NLS-1$
			data.add(new BasicNameValuePair("redirect_uri", RepositoryPropertyKey.BaseURL.getValue(
					RepositoryPropertyKey.BaseURL.getDefaultValue()))); //$NON-NLS-1$
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

			// we're actually not interested in the access token 
			// but we want the email information from the id token
			String idToken = accessTokenResponseJSon.get("id_token").asText(); //$NON-NLS-1$
			
			String[] pieces = idToken.split("\\."); //$NON-NLS-1$
			// we skip the header and go ahead with the payload
			String payload = pieces[1];

			String decodedPayload = 
					new String(Base64.decodeBase64(payload), "UTF-8"); //$NON-NLS-1$
			ObjectNode payloadJson = mapper.readValue(decodedPayload, ObjectNode.class);
			
			logger.info("decodedPayload: " + decodedPayload); //$NON-NLS-1$
			
			// finally the email address
			// String email = payloadJson.get("email").asText(); //$NON-NLS-1$

			String identifier = payloadJson.get("sub").asText();
			String name = payloadJson.get("name") == null ? identifier : payloadJson.get("name").asText();
			String email = payloadJson.get("email").asText();
			String provider = "google.com";
			loginservice.loggedInFromThirdParty(identifier, provider, email, name);
			setAttribute("OAUTHTOKEN", null);
		}
		else {
            logger.info("authentication failure: " + error); //$NON-NLS-1$
			new Notification(
                Messages.getString("AuthenticationDialog.authFailureTitle"), //$NON-NLS-1$
                Messages.getString("AuthenticationDialog.authFailureMessage"), //$NON-NLS-1$
                Type.ERROR_MESSAGE).show(getPage());
		}
		} catch (Exception e) {
			e.printStackTrace();
			((ErrorHandler)this).showAndLogError(
					Messages.getString("AuthenticationDialog.errorOpeningRepo"), e); //$NON-NLS-1$
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
	
	@Subscribe
	public void handleClosableResources(RegisterCloseableEvent registerCloseableEvent ){
		closeListener.add(new WeakReference<Closeable>(registerCloseableEvent.getCloseable()));
	}
	
}
