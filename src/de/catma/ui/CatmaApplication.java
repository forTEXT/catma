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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.LogProgressListener;
import de.catma.backgroundservice.ProgressCallable;
import de.catma.document.Corpus;
import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryProperties;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.source.KeywordInContext;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.indexer.IndexedRepository;
import de.catma.project.ProjectManager;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.queryengine.result.computation.DistributionSelectionListener;
import de.catma.repository.LoginToken;
import de.catma.repository.db.maintenance.UserManager;
import de.catma.repository.git.GitProjectManager;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.analyzer.AnalyzerProvider;
import de.catma.ui.analyzer.QueryOptionsProvider;
import de.catma.ui.authentication.AuthenticationHandler;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.project.ProjectManagerView;
import de.catma.ui.tagger.TaggerView;
import de.catma.ui.tagmanager.TagsetSelectionListener;
import de.catma.user.User;

//@Push(PushMode.MANUAL)
@Theme("catma")
@PreserveOnRefresh
public class CatmaApplication extends UI implements BackgroundServiceProvider, AnalyzerProvider, LoginToken, ParameterProvider {

	private static final String MINORVERSION = "(build " + new SimpleDateFormat("yyyy/MM/dd-HH:mm").format(new Date()) //$NON-NLS-1$ //$NON-NLS-2$
			+ ")"; //$NON-NLS-1$
	private static final String WEB_INF_DIR = "WEB-INF"; //$NON-NLS-1$

	private String tempDirectory = null;
	private BackgroundService backgroundService;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Map<String, String[]> parameters = new HashMap<String, String[]>();
	private boolean repositoryOpened = false;
	
	private UserManager userManager = new UserManager();
	private Object user;
	
	private VerticalLayout mainLayout;
	private Panel contentPanel;

	private HorizontalLayout menuLayout;
	private Panel menuPanel;
	
	private Button btHelp;

	private ThemeResource logoResource;

	private Label defaultContentPanelLabel;

	private UIHelpWindow uiHelpWindow = new UIHelpWindow();
	private Button btloginLogout;
	
	private ProjectManagerView projectManagerView;

	public CatmaApplication() {
	}

	@Override
	protected void init(VaadinRequest request) {
		logger.info("Session: " + request.getWrappedSession().getId());
		storeParameters(request.getParameterMap());

		Page.getCurrent().setTitle("CATMA 6.0 " + MINORVERSION); //$NON-NLS-1$

		mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();

		menuPanel = new Panel();
		menuPanel.addStyleName("menuPanel"); //$NON-NLS-1$
		mainLayout.addComponent(menuPanel);

		contentPanel = new Panel();
		contentPanel.setHeight("100%"); //$NON-NLS-1$
		contentPanel.addStyleName("contentPanel"); //$NON-NLS-1$

		defaultContentPanelLabel = new Label(Messages.getString("CatmaApplication.logInToGetStarted")); //$NON-NLS-1$
		defaultContentPanelLabel.addStyleName("defaultContentPanelLabel"); //$NON-NLS-1$
		contentPanel.setContent(defaultContentPanelLabel);

		mainLayout.addComponent(contentPanel);
		mainLayout.setExpandRatio(contentPanel, 1.0f);

		menuLayout = new HorizontalLayout();
		menuLayout.setMargin(true);
		menuLayout.setSpacing(true);

		logoResource = new ThemeResource("catma-logo.png"); //$NON-NLS-1$
		Link logoImage = new Link(null, new ExternalResource("http://www.catma.de")); //$NON-NLS-1$
		logoImage.setIcon(logoResource);
		logoImage.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(logoImage);
		
		
		Link latestFeaturesLink = new Link(Messages.getString("CatmaApplication.latestFeatures"), //$NON-NLS-1$
				new ExternalResource("http://www.catma.de/latestfeatures")); //$NON-NLS-1$
		latestFeaturesLink.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(latestFeaturesLink);
		menuLayout.setComponentAlignment(latestFeaturesLink, Alignment.TOP_RIGHT);
		menuLayout.setExpandRatio(latestFeaturesLink, 1.0f);

		Link aboutLink = new Link(Messages.getString("CatmaApplication.about"), //$NON-NLS-1$
				new ExternalResource("http://www.catma.de")); //$NON-NLS-1$
		aboutLink.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(aboutLink);
		menuLayout.setComponentAlignment(aboutLink, Alignment.TOP_RIGHT);

		Link termsOfUseLink = new Link(Messages.getString("CatmaApplication.termsOfUse"), //$NON-NLS-1$
				new ExternalResource("http://www.catma.de/termsofuse")); //$NON-NLS-1$
		termsOfUseLink.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(termsOfUseLink);
		menuLayout.setComponentAlignment(termsOfUseLink, Alignment.TOP_RIGHT);

		Link manualLink = new Link(Messages.getString("CatmaApplication.Manual"), //$NON-NLS-1$
				new ExternalResource(request.getContextPath() + "/manual/")); //$NON-NLS-1$
		manualLink.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(manualLink);
		menuLayout.setComponentAlignment(manualLink, Alignment.TOP_RIGHT);

		Link helpLink = new Link(Messages.getString("CatmaApplication.Helpdesk"), //$NON-NLS-1$
				new ExternalResource("http://www.catma.de/helpdesk/")); //$NON-NLS-1$
		helpLink.setTargetName("_blank"); //$NON-NLS-1$
		menuLayout.addComponent(helpLink);
		menuLayout.setComponentAlignment(helpLink, Alignment.TOP_RIGHT);
		helpLink.setVisible(false);

		btHelp = new Button(FontAwesome.QUESTION_CIRCLE);
		btHelp.addStyleName("help-button"); //$NON-NLS-1$
		btHelp.addStyleName("application-help-button"); //$NON-NLS-1$

		menuLayout.addComponent(btHelp);

		btHelp.addClickListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {

				if (uiHelpWindow.getParent() == null) {
					UI.getCurrent().addWindow(uiHelpWindow);
				} else {
					UI.getCurrent().removeWindow(uiHelpWindow);
				}

			}
		});

		btloginLogout = new Button(Messages.getString("CatmaApplication.signIn"), //$NON-NLS-1$
				event -> handleLoginLogoutEvent(event));
		btloginLogout.setStyleName(BaseTheme.BUTTON_LINK);
		btloginLogout.addStyleName("application-loginlink"); //$NON-NLS-1$

		menuLayout.addComponent(btloginLogout);
		menuLayout.setComponentAlignment(btloginLogout, Alignment.TOP_RIGHT);
		menuLayout.setWidth("100%"); //$NON-NLS-1$

		menuPanel.setContent(menuLayout);

		setContent(mainLayout);
		

		try {


//			menu = menuFactory.createMenu(menuLayout, contentPanel,
//					new MenuFactory.MenuEntryDefinition(Messages.getString("CatmaApplication.repositoryManager"), //$NON-NLS-1$
//							repositoryManagerView),
//					new MenuFactory.MenuEntryDefinition(Messages.getString("CatmaApplication.tagManager"), //$NON-NLS-1$
//							tagManagerView),
//					new MenuFactory.MenuEntryDefinition(Messages.getString("CatmaApplication.tagger"), //$NON-NLS-1$
//							taggerManagerView),
//					new MenuFactory.MenuEntryDefinition(Messages.getString("CatmaApplication.analyzer"), //$NON-NLS-1$
//							analyzerManagerView),
//					new MenuFactory.MenuEntryDefinition(Messages.getString("CatmaApplication.visualizer"), //$NON-NLS-1$
//							visualizationManagerView));
//			addPropertyChangeListener(CatmaApplicationEvent.userChange, menu.userChangeListener);


			if (getParameter(Parameter.USER_IDENTIFIER) != null) {
				btloginLogout.click();
			}

			setPollInterval(10000);

			
//TODO:
//			if ((getParameter(Parameter.AUTOLOGIN) != null) && (getUser() == null)) {
//				getPage().setLocation(repositoryManagerView.createAuthenticationDialog().createLogInClick(this,
//						RepositoryPropertyKey.CATMA_oauthAuthorizationCodeRequestURL.getValue(),
//						RepositoryPropertyKey.CATMA_oauthAccessTokenRequestURL.getValue(),
//						RepositoryPropertyKey.CATMA_oauthClientId.getValue(),
//						RepositoryPropertyKey.CATMA_oauthClientSecret.getValue(), URLEncoder.encode("/", "UTF-8"))); //$NON-NLS-1$ //$NON-NLS-2$
//			}
//
//			if (VaadinSession.getCurrent().getAttribute(SessionKey.USER.name()) != null) {
//				logger.info("auto opening repo for: " + VaadinSession.getCurrent().getAttribute(SessionKey.USER.name()));
//				repositoryManagerView.openFirstRepository(this, (Map<String, String>) VaadinSession.getCurrent().getAttribute(SessionKey.USER.name()));
////				menu.executeEntry(repositoryManagerView);// TODO:
//			}
//			else {
//				logger.info("SessionKey.USER not set");
//			}
			
		} catch (Exception e) {
			showAndLogError(Messages.getString("CatmaApplication.errorSystemNotInitialized"), e); //$NON-NLS-1$
		}

	}

	private void handleLoginLogoutEvent(ClickEvent event) {
		
		String scheme = VaadinServletService.getCurrentServletRequest().getScheme();		
		String serverName = VaadinServletService.getCurrentServletRequest().getServerName();		
		Integer port = VaadinServletService.getCurrentServletRequest().getServerPort();
		String contextPath = VaadinService.getCurrentRequest().getContextPath();
		
		final String afterLogoutRedirectURL = 
				String.format("%s://%s%s%s", scheme, serverName, port == 80 ? "" : ":"+port, contextPath); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		
		if (this.user == null) {
			
			AuthenticationHandler authenticationHandler = 
					new AuthenticationHandler();
			
			authenticationHandler.authenticate(userIdentification -> {
				try {
					
					this.user = userIdentification;
					userManager.login(this);

					backgroundService = new UIBackgroundService(true);

					initTempDirectory();
					
					btloginLogout.setHtmlContentAllowed(true);
					
					ProjectManager projectManager = 
						new GitProjectManager(
							RepositoryPropertyKey.GitLabServerUrl.getValue(),
							RepositoryPropertyKey.GitLabAdminPersonalAccessToken.getValue(), 
							RepositoryPropertyKey.GitBasedRepositoryBasePath.getValue(),
							userIdentification);
					
					User user = projectManager.getUser();
										
					//TODO:
//					if (user.isGuest()) {
//						identifier = Messages.getString("CatmaApplication.Guest"); //$NON-NLS-1$
//					}

					btloginLogout.setCaption(
						MessageFormat.format(
							Messages.getString("CatmaApplication.signOut"), user.getName())); //$NON-NLS-1$

					projectManagerView = new ProjectManagerView(projectManager);
					
					contentPanel.setContent(projectManagerView);
					
				} catch (Exception e) {
					showAndLogError(Messages.getString("CatmaApplication.errorSystemNotInitialized"), e); //$NON-NLS-1$
				}
			});
		}
		else {
			btloginLogout.setCaption(Messages.getString("CatmaApplication.signIn")); //$NON-NLS-1$
			
			logger.info("closing session and redirecting to " + afterLogoutRedirectURL);
			Page.getCurrent().setLocation(afterLogoutRedirectURL);
			VaadinSession.getCurrent().close();
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

	private void initTempDirectory() throws IOException {
		String tempDirProp = RepositoryPropertyKey.TempDir.getValue();
		File tempDir = new File(tempDirProp);

		if (!tempDir.isAbsolute()) {
			this.tempDirectory = VaadinServlet.getCurrent().getServletContext().getRealPath(// TODO:
																							// check
					WEB_INF_DIR + System.getProperty("file.separator") //$NON-NLS-1$
							+ tempDirProp);
		} else {
			this.tempDirectory = tempDirProp;
		}

		tempDir = new File(this.tempDirectory);
		if ((!tempDir.exists() && !tempDir.mkdirs())) {
			throw new IOException("could not create temporary directory: " + this.tempDirectory); //$NON-NLS-1$
		}
	}
	
	public void addTagsetToActiveDocument(TagsetDefinition tagsetDefinition, TagsetSelectionListener tagsetSelectionListener) {
		//TODO:
//		TaggerView selectedTab = (TaggerView) taggerManagerView.getSelectedTab();
//
//		if (selectedTab == null) {
//			Notification.show(Messages.getString("CatmaApplication.infoTitle"), //$NON-NLS-1$
//					Messages.getString("CatmaApplication.noActiveDocumentInTagger"), //$NON-NLS-1$
//					Type.TRAY_NOTIFICATION);
//			return;
//		}
//
//		selectedTab.openTagsetDefinition(this, tagsetDefinition,tagsetSelectionListener);	
//
//		SourceDocument sd = selectedTab.getSourceDocument();
//		String sourceDocumentCaption = sd.toString();
//
//		Notification.show(Messages.getString("CatmaApplication.infoTitle"), //$NON-NLS-1$
//				MessageFormat.format(Messages.getString("CatmaApplication.tagsetLoaded"), sourceDocumentCaption), //$NON-NLS-1$
//				Type.TRAY_NOTIFICATION);
	}
	
	public void addTagsetToActiveDocument(TagsetDefinition tagsetDefinition) {
		addTagsetToActiveDocument(tagsetDefinition, null);
	}

	public void openTagLibrary(Repository repository, TagLibrary tagLibrary) {
		openTagLibrary(repository, tagLibrary, true);
	}

	public void openTagLibrary(Repository repository, TagLibrary tagLibrary, boolean switchToTagManagerView) {
		projectManagerView.openTagLibrary(repository, tagLibrary, switchToTagManagerView);
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

	public TaggerView openSourceDocument(SourceDocument sourceDocument, Repository repository) {
		return projectManagerView.openSourceDocument(sourceDocument, repository);
	}

	public String getTempDirectory() {
		return tempDirectory;
	}

	public BackgroundService getBackgroundService() {
		return backgroundService;
	}

	public <T> void submit(String caption, final ProgressCallable<T> callable, final ExecutionListener<T> listener) {
		logger.info("submitting job '" + caption + "' " + callable); //$NON-NLS-1$ //$NON-NLS-2$
		getBackgroundService().submit(callable, new ExecutionListener<T>() {
			public void done(T result) {
				listener.done(result);
			};

			public void error(Throwable t) {
				listener.error(t);
			}
		}, new LogProgressListener());
	}

	public void openUserMarkupCollection(SourceDocument sourceDocument, UserMarkupCollection userMarkupCollection,
			Repository repository) {
		projectManagerView.openUserMarkupCollection(sourceDocument, userMarkupCollection, repository);
	}

	public void analyze(Corpus corpus, IndexedRepository repository) {
		projectManagerView.analyze(corpus, repository);
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
		VaadinSession.getCurrent().setAttribute("USER", null);
		if (projectManagerView != null) {
			projectManagerView.close();
		}
		logger.info("application for user" + getUser() + " has been closed"); //$NON-NLS-1$ //$NON-NLS-2$
		if (repositoryOpened) {
			userManager.logout(this);
			repositoryOpened = false;
		}
		backgroundService.shutdown();
		super.close();
	}

	public void showAndLogError(String message, Throwable e) {
		logger.log(Level.SEVERE, "[" + getUser() + "]" + message, e); //$NON-NLS-1$ //$NON-NLS-2$

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
	public Object getUser() {
		return user;
	}


}
