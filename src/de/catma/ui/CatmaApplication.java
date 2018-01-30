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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
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
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Notification;
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
import de.catma.document.repository.RepositoryManager;
import de.catma.document.repository.RepositoryProperties;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.source.KeywordInContext;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.indexer.IndexedRepository;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.queryengine.result.computation.DistributionSelectionListener;
import de.catma.repository.LoginToken;
import de.catma.repository.db.maintenance.UserManager;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.analyzer.AnalyzerManagerView;
import de.catma.ui.analyzer.AnalyzerProvider;
import de.catma.ui.analyzer.QueryOptionsProvider;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.menu.LoginLogoutCommand;
import de.catma.ui.menu.MainMenu;
import de.catma.ui.menu.MenuFactory;
import de.catma.ui.repository.RepositoryManagerView;
import de.catma.ui.tagger.TaggerManagerView;
import de.catma.ui.tagger.TaggerView;
import de.catma.ui.tagmanager.TagManagerView;
import de.catma.ui.tagmanager.TagsetSelectionListener;
import de.catma.ui.visualizer.VisualizationManagerView;

//@Push(PushMode.MANUAL)
@Theme("catma")
@PreserveOnRefresh
public class CatmaApplication extends UI implements BackgroundServiceProvider, AnalyzerProvider, LoginToken {

	private static final String MINORVERSION = "(build " + new SimpleDateFormat("yyyy/MM/dd-HH:mm").format(new Date()) //$NON-NLS-1$ //$NON-NLS-2$
			+ ")"; //$NON-NLS-1$
	private static final String WEB_INF_DIR = "WEB-INF"; //$NON-NLS-1$

	private RepositoryManagerView repositoryManagerView;
	private TagManagerView tagManagerView;
	private MainMenu menu;
	private String tempDirectory = null;
	private BackgroundService backgroundService;
	private TaggerManagerView taggerManagerView;
	private AnalyzerManagerView analyzerManagerView;
	private TagManager tagManager;
	private VisualizationManagerView visualizationManagerView;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Map<String, String[]> parameters = new HashMap<String, String[]>();
	private boolean repositoryOpened = false;
	private UserManager userManager = new UserManager();
	private Object user;
	private VerticalLayout mainLayout;
	private Panel menuPanel;
	private HorizontalLayout menuLayout;
	private Panel contentPanel;
	private Button btHelp;

	private ThemeResource logoResource;

	private Label defaultContentPanelLabel;

	private PropertyChangeSupport propertyChangeSupport;

	private UIHelpWindow uiHelpWindow = new UIHelpWindow();

	/**
	 * Events emitted by the CatmaApplication.
	 * 
	 * @see CatmaApplication#addPropertyChangeListener(CatmaApplicationEvent,
	 *      PropertyChangeListener)
	 */
	public static enum CatmaApplicationEvent {
		/**
		 * <p>
		 * User logged in:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = {@link Object}</li>
		 * </p>
		 * <br />
		 * <p>
		 * User logged out:
		 * <li>{@link PropertyChangeEvent#getNewValue()} =
		 * <code>null</code></li>
		 * </p>
		 * <br />
		 */
		userChange,;
	}

	public CatmaApplication() {
		propertyChangeSupport = new PropertyChangeSupport(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void init(VaadinRequest request) {
		backgroundService = new UIBackgroundService(true);
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

		MenuFactory menuFactory = new MenuFactory();
		try {

			initTempDirectory();
			tagManager = new TagManager();

			repositoryManagerView = new RepositoryManagerView(
					new RepositoryManager(this, tagManager, RepositoryProperties.INSTANCE.getProperties()));

			tagManagerView = new TagManagerView(tagManager);

			taggerManagerView = new TaggerManagerView();

			analyzerManagerView = new AnalyzerManagerView();

			visualizationManagerView = new VisualizationManagerView();

			menu = menuFactory.createMenu(menuLayout, contentPanel,
					new MenuFactory.MenuEntryDefinition(Messages.getString("CatmaApplication.repositoryManager"), //$NON-NLS-1$
							repositoryManagerView),
					new MenuFactory.MenuEntryDefinition(Messages.getString("CatmaApplication.tagManager"), //$NON-NLS-1$
							tagManagerView),
					new MenuFactory.MenuEntryDefinition(Messages.getString("CatmaApplication.tagger"), //$NON-NLS-1$
							taggerManagerView),
					new MenuFactory.MenuEntryDefinition(Messages.getString("CatmaApplication.analyzer"), //$NON-NLS-1$
							analyzerManagerView),
					new MenuFactory.MenuEntryDefinition(Messages.getString("CatmaApplication.visualizer"), //$NON-NLS-1$
							visualizationManagerView));
			addPropertyChangeListener(CatmaApplicationEvent.userChange, menu.userChangeListener);

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

			LoginLogoutCommand loginLogoutCommand = new LoginLogoutCommand(menu, repositoryManagerView);
			Button btloginLogout = new Button(Messages.getString("CatmaApplication.signIn"), //$NON-NLS-1$
					event -> loginLogoutCommand.menuSelected(null));
			btloginLogout.setStyleName(BaseTheme.BUTTON_LINK);
			btloginLogout.addStyleName("application-loginlink"); //$NON-NLS-1$

			loginLogoutCommand.setLoginLogoutButton(btloginLogout);

			menuLayout.addComponent(btloginLogout);
			menuLayout.setComponentAlignment(btloginLogout, Alignment.TOP_RIGHT);
			menuLayout.setWidth("100%"); //$NON-NLS-1$

			menuPanel.setContent(menuLayout);

			setContent(mainLayout);

			if (getParameter(Parameter.USER_IDENTIFIER) != null) {
				btloginLogout.click();
			}

			setPollInterval(10000);

			if ((getParameter(Parameter.AUTOLOGIN) != null) && (getUser() == null)) {
				getPage().setLocation(repositoryManagerView.createAuthenticationDialog().createLogInClick(this,
						RepositoryPropertyKey.CATMA_oauthAuthorizationCodeRequestURL.getValue(),
						RepositoryPropertyKey.CATMA_oauthAccessTokenRequestURL.getValue(),
						RepositoryPropertyKey.CATMA_oauthClientId.getValue(),
						RepositoryPropertyKey.CATMA_oauthClientSecret.getValue(), URLEncoder.encode("/", "UTF-8"))); //$NON-NLS-1$ //$NON-NLS-2$
			}

			if (VaadinSession.getCurrent().getAttribute(SessionKey.USER.name()) != null) {
				logger.info("auto opening repo for: " + VaadinSession.getCurrent().getAttribute(SessionKey.USER.name()));
				repositoryManagerView.openFirstRepository(this, (Map<String, String>) VaadinSession.getCurrent().getAttribute(SessionKey.USER.name()));
				menu.executeEntry(repositoryManagerView);
			}
			else {
				logger.info("SessionKey.USER not set");
			}
			
		} catch (Exception e) {
			showAndLogError(Messages.getString("CatmaApplication.errorSystemNotInitialized"), e); //$NON-NLS-1$
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
		TaggerView selectedTab = (TaggerView) taggerManagerView.getSelectedTab();

		if (selectedTab == null) {
			Notification.show(Messages.getString("CatmaApplication.infoTitle"), //$NON-NLS-1$
					Messages.getString("CatmaApplication.noActiveDocumentInTagger"), //$NON-NLS-1$
					Type.TRAY_NOTIFICATION);
			return;
		}

		selectedTab.openTagsetDefinition(this, tagsetDefinition,tagsetSelectionListener);	

		SourceDocument sd = selectedTab.getSourceDocument();
		String sourceDocumentCaption = sd.toString();

		Notification.show(Messages.getString("CatmaApplication.infoTitle"), //$NON-NLS-1$
				MessageFormat.format(Messages.getString("CatmaApplication.tagsetLoaded"), sourceDocumentCaption), //$NON-NLS-1$
				Type.TRAY_NOTIFICATION);
	}
	
	public void addTagsetToActiveDocument(TagsetDefinition tagsetDefinition) {
		addTagsetToActiveDocument(tagsetDefinition, null);
	}

	public void openRepository(Repository repository) {
		repositoryOpened = true;
		userManager.login(this);
		repositoryManagerView.openRepository(repository);
		logger.info("repository has been opened for user" + getUser()); //$NON-NLS-1$
	}

	public void openTagLibrary(Repository repository, TagLibrary tagLibrary) {
		openTagLibrary(repository, tagLibrary, true);
	}

	public void openTagLibrary(Repository repository, TagLibrary tagLibrary, boolean switchToTagManagerView) {
		if (switchToTagManagerView) {
			menu.executeEntry(tagManagerView);
		}
		tagManagerView.openTagLibrary(this, repository, tagLibrary);
	}

	public TaggerView openSourceDocument(String sourceDocumentId) {

		RepositoryManager repositoryManager = repositoryManagerView.getRepositoryManager();

		if (repositoryManager.hasOpenRepository()) {
			Repository repository = repositoryManager.getFirstOpenRepository();

			SourceDocument sourceDocument = repository.getSourceDocument(sourceDocumentId);
			if (sourceDocument != null) {
				return openSourceDocument(sourceDocument, repository);
			}
		}

		return null;
	}

	public TaggerView openSourceDocument(SourceDocument sourceDocument, Repository repository) {

		menu.executeEntry(taggerManagerView);

		return taggerManagerView.openSourceDocument(sourceDocument, repository);
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
		TaggerView taggerView = openSourceDocument(sourceDocument, repository);
		taggerManagerView.openUserMarkupCollection(taggerView, userMarkupCollection);

	}

	public void analyze(Corpus corpus, IndexedRepository repository) {
		menu.executeEntry(analyzerManagerView);
		analyzerManagerView.analyzeDocuments(corpus, repository);
	}

	public void analyzeCurrentlyActiveDocument() {
		menu.executeEntry(analyzerManagerView);
		
		Component selectedTab = taggerManagerView.getSelectedTab();
		
		if (selectedTab != null) {
			TaggerView taggerView = (TaggerView) selectedTab;
			taggerView.analyzeDocument();
		} else {
			Notification.show(Messages.getString("CatmaApplication.noOpenDocument"), Type.TRAY_NOTIFICATION); //$NON-NLS-1$
		}
	}
	
	

	public int addVisualization(Integer visualizationId, String caption,
			DistributionComputation distributionComputation,
			DistributionSelectionListener distributionSelectionListener) {

		menu.executeEntry(visualizationManagerView);

		return visualizationManagerView.addVisualization(visualizationId, caption, distributionComputation,
				distributionSelectionListener);
	}

	@Override
	public void close() {
		VaadinSession.getCurrent().setAttribute("USER", null);
		repositoryManagerView.getRepositoryManager().close();
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
		menu.executeEntry(visualizationManagerView);
		visualizationManagerView.addDoubleTree(kwics);
	}

	public void addVega(QueryResult queryResult, QueryOptionsProvider queryOptionsProvider) {
		menu.executeEntry(visualizationManagerView);
		visualizationManagerView.addVega(queryResult, queryOptionsProvider);
	}

	@Override
	public Object getUser() {
		return user;
	}

	public void setUser(Object newUser) {
		logger.info("Setting user-> new: " + newUser + " old:" + this.user);
		
		Object currentUser = this.user;
		this.user = newUser;
		
		propertyChangeSupport.firePropertyChange(CatmaApplicationEvent.userChange.name(), currentUser, newUser);
	}

	public MainMenu getMenu() {
		return menu;
	}

	@Override
	public void attach() {
		super.attach();
	}

	/**
	 * @see CatmaApplicationEvent
	 * @see PropertyChangeSupport#addPropertyChangeListener(String,
	 *      PropertyChangeListener)
	 */
	public void addPropertyChangeListener(CatmaApplicationEvent propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName.name(), listener);
	}

	/**
	 * @see CatmaApplicationEvent
	 * @see PropertyChangeSupport#removePropertyChangeListener(String,
	 *      PropertyChangeListener)
	 */
	public void removePropertyChangeListener(CatmaApplicationEvent propertyName, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName.name(), listener);
	}



}
