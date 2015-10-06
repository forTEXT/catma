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
import com.vaadin.server.ClassResource;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

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
import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.queryengine.result.computation.DistributionSelectionListener;
import de.catma.repository.LoginToken;
import de.catma.repository.db.maintenance.UserManager;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.ui.analyzer.AnalyzerManagerView;
import de.catma.ui.analyzer.AnalyzerManagerWindow;
import de.catma.ui.analyzer.AnalyzerProvider;
import de.catma.ui.component.HTMLNotification;
import de.catma.ui.menu.LoginLogoutCommand;
import de.catma.ui.menu.Menu;
import de.catma.ui.menu.MenuFactory;
import de.catma.ui.repository.RepositoryManagerView;
import de.catma.ui.repository.RepositoryManagerWindow;
import de.catma.ui.tagger.TaggerManagerView;
import de.catma.ui.tagger.TaggerManagerWindow;
import de.catma.ui.tagger.TaggerView;
import de.catma.ui.tagmanager.TagManagerView;
import de.catma.ui.tagmanager.TagManagerWindow;
import de.catma.ui.visualizer.VisualizationManagerView;
import de.catma.ui.visualizer.VisualizationManagerWindow;

//@Push(value=PushMode.MANUAL, transport=Transport.LONG_POLLING)
@Theme("cleatheme")
@PreserveOnRefresh
public class CatmaApplication extends UI
	implements BackgroundServiceProvider, AnalyzerProvider, LoginToken {
	
	private static final String MINORVERSION = 
			"(v"+new SimpleDateFormat("yyyy/MM/dd-HH:mm").format(new Date())+")";
	private static final String WEB_INF_DIR = "WEB-INF";

	private RepositoryManagerView repositoryManagerView;
	private TagManagerView tagManagerView;
	private Menu menu;
	private String tempDirectory = null;
	private BackgroundService backgroundService;
	private TaggerManagerView taggerManagerView;
	private AnalyzerManagerView analyzerManagerView;
	private TagManager tagManager;
	private VisualizationManagerView visualizationManagerView;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Map<String,String[]> parameters = new HashMap<String, String[]>();
	private boolean repositoryOpened = false;
	private UserManager userManager = new UserManager();
	private Object user;
	private HorizontalLayout mainLayout;

	private PropertyChangeSupport propertyChangeSupport;
	
	/**
	 * Events emitted by the CatmaApplication.
	 * 
	 * @see CatmaApplication#addPropertyChangeListener(CatmaApplicationEvent, PropertyChangeListener)
	 */
	public static enum CatmaApplicationEvent {
		/**
		 * <p>User logged in:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = {@link Object}</li>
		 * </p><br />
		 * <p>User logged out:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = <code>null</code></li>
		 * </p><br />
		 */
		userChange,
		;
	}
	
	public CatmaApplication() {
		propertyChangeSupport = new PropertyChangeSupport(this);
	}

	@Override
	protected void init(VaadinRequest request) {
		backgroundService = new UIBackgroundService(true);
		
		handleParameters(request.getParameterMap());
		
		Page.getCurrent().setTitle("CATMA 4.2 - CLÉA " + MINORVERSION);
		
		mainLayout = new HorizontalLayout();
		mainLayout.setSizeUndefined();
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);
		addStyleName("catma-mainwindow");
		
		MenuFactory menuFactory = new MenuFactory();
		try {

			initTempDirectory();
			tagManager = new TagManager();
			
			repositoryManagerView = 
				new RepositoryManagerView(
					new RepositoryManager(
						this, tagManager, 
						RepositoryProperties.INSTANCE.getProperties()));
		
			tagManagerView = new TagManagerView(tagManager);
			
			taggerManagerView = new TaggerManagerView();
			
			analyzerManagerView = new AnalyzerManagerView();
			
			visualizationManagerView = new VisualizationManagerView();
			
			menu = menuFactory.createMenu(
					mainLayout, 
					new MenuFactory.MenuEntryDefinition( 
							"Repository Manager",
							new RepositoryManagerWindow(repositoryManagerView)),
					new MenuFactory.MenuEntryDefinition(
							"Tag Manager",
							new TagManagerWindow(tagManagerView)),
					new MenuFactory.MenuEntryDefinition(
							"Tagger",
							new TaggerManagerWindow(taggerManagerView)),
					new MenuFactory.MenuEntryDefinition(
							"Analyzer",
							new AnalyzerManagerWindow(analyzerManagerView)),
					new MenuFactory.MenuEntryDefinition(
							"Visualizer",
							new VisualizationManagerWindow(visualizationManagerView))
					);
			addPropertyChangeListener(CatmaApplicationEvent.userChange, menu.userChangeListener);
			
			Link latestFeaturesLink = new Link(
					"Latest Features", new ExternalResource("http://www.catma.de/latestfeatures"));
			latestFeaturesLink.setTargetName("_blank");
			mainLayout.addComponent(latestFeaturesLink);
			mainLayout.setComponentAlignment(latestFeaturesLink, Alignment.TOP_RIGHT);
			mainLayout.setExpandRatio(latestFeaturesLink, 1.0f);
			
			Link aboutLink = new Link(
					"About", new ExternalResource("http://www.catma.de"));
			aboutLink.setTargetName("_blank");
			mainLayout.addComponent(aboutLink);
			mainLayout.setComponentAlignment(aboutLink, Alignment.TOP_RIGHT);
			
			Link termsOfUseLink = new Link(
					"Terms of Use", new ExternalResource("http://www.catma.de/termsofuse"));
			termsOfUseLink.setTargetName("_blank");
			mainLayout.addComponent(termsOfUseLink);
			mainLayout.setComponentAlignment(termsOfUseLink, Alignment.TOP_RIGHT);

			Link helpLink = new Link(
					"Help", 
					new ExternalResource(request.getContextPath()+"/manual/"));
			helpLink.setTargetName("_blank");
			mainLayout.addComponent(helpLink);
			mainLayout.setComponentAlignment(helpLink, Alignment.TOP_RIGHT);
			
			final Label helpLabel = new Label();
			helpLabel.setIcon(new ClassResource("resources/icon-help.gif"));
			helpLabel.setWidth("20px");
			helpLabel.setDescription(
					"<h3>Hints</h3>" +
					"<p>Watch out for these little question mark icons while navigating " +
					"through CATMA. They provide useful hints for managing the first " +
					"steps within a CATMA component.</p>" +
					"<h4>Login</h4>" +
					"Once you're logged in, you will see the Repository Manager, " +
					"which will explain the first steps to you. " +
					"Just hover your mouse over the question mark icons!");

			mainLayout.addComponent(helpLabel);
			
			MenuBar loginLogoutMenu = new MenuBar();
			LoginLogoutCommand loginLogoutCommand = 
					new LoginLogoutCommand(menu, repositoryManagerView);
			MenuItem loginLogoutitem = loginLogoutMenu.addItem("Login", loginLogoutCommand);
			loginLogoutCommand.setLoginLogoutItem(loginLogoutitem);
			
			mainLayout.addComponent(loginLogoutMenu);
			mainLayout.setComponentAlignment(loginLogoutMenu, Alignment.TOP_RIGHT);
			mainLayout.setWidth("100%");

			setContent(mainLayout);

			setPollInterval(1000);
			
		} catch (Exception e) {
			showAndLogError("The system could not be initialized!", e);
		}

	}
	
	public void handleParameters(Map<String, String[]> parameters) {
		this.parameters.putAll(parameters);
	}
	
	public Map<String, String[]> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}
	
	public String getParameter(String key) {
		String[] values = parameters.get(key);
		if ((values != null) && (values.length > 0)) {
			return values[0];
		}
		
		return null;
	}
	
	private void initTempDirectory() throws IOException {
		String tempDirProp = RepositoryPropertyKey.TempDir.getValue();
		File tempDir = new File(tempDirProp);

		if (!tempDir.isAbsolute()) {
			this.tempDirectory = 
					VaadinServlet.getCurrent().getServletContext().getRealPath(//TODO: check
					WEB_INF_DIR
					+ System.getProperty("file.separator")
					+ tempDirProp);
		}
		else {
			this.tempDirectory = tempDirProp;
		}
		
		tempDir = new File(this.tempDirectory);
		if ((!tempDir.exists() && !tempDir.mkdirs())) {
			throw new IOException(
				"could not create temporary directory: " + this.tempDirectory);
		}
	}
	
	public void addTagsetToActiveDocument(TagsetDefinition tagsetDefinition){
		TaggerView selectedTab = (TaggerView)taggerManagerView.getSelectedTab();
		
		if(selectedTab == null){
			Notification.show(
					"Information", 
					"There is no active document open in the Tagger",
					Type.TRAY_NOTIFICATION);
			return;
		}
		
		selectedTab.openTagsetDefinition(tagsetDefinition);
		
		SourceDocument sd = selectedTab.getSourceDocument();
		String sourceDocumentCaption = sd.toString();
		
		Notification.show(
				"Information", 
				"Tagset loaded into active document: '" + sourceDocumentCaption + "'",
				Type.TRAY_NOTIFICATION);
	}

	public void openRepository(Repository repository) {
		repositoryOpened = true;
		userManager.login(this);
		repositoryManagerView.openRepository(repository);
		logger.info("repository has been opened for user" + getUser());
	}
	 
	public void openTagLibrary(Repository repository, TagLibrary tagLibrary) {
		if (!tagManagerView.isAttached()) {
			menu.executeEntry(tagManagerView);
		}
		else {
			((Window)tagManagerView.getParent()).bringToFront();
		}
		tagManagerView.openTagLibrary(repository, tagLibrary);
	}

	public TaggerView openSourceDocument(
			SourceDocument sourceDocument, Repository repository) {
		if (!taggerManagerView.isAttached()) {
			menu.executeEntry(taggerManagerView);
			Notification.show(
					"Information", 
					"To markup your text please drag Tagsets from a Tag Library " +
					"into the currently active Tagsets area!",
					Type.TRAY_NOTIFICATION);
		}
		else {
			((Window)taggerManagerView.getParent()).bringToFront();
		}
		return taggerManagerView.openSourceDocument(
				sourceDocument, repository);
	}

	public String getTempDirectory() {
		return tempDirectory;
	}
	
	public BackgroundService getBackgroundService() {
		return backgroundService;
	}
	
	public <T> void submit( 
			String caption,
			final ProgressCallable<T> callable, 
			final ExecutionListener<T> listener) {
		logger.info("submitting job '" + caption +  "' " + callable);
		getBackgroundService().submit(
			callable, new ExecutionListener<T>() {
				public void done(T result) {
					listener.done(result);
				};
				
				public void error(Throwable t) {
					listener.error(t);
				}
			}, 
			new LogProgressListener());
	}
	

	public void openUserMarkupCollection(
			SourceDocument sourceDocument, 
			UserMarkupCollection userMarkupCollection, Repository repository) {
		TaggerView taggerView = openSourceDocument(sourceDocument, repository);
		taggerManagerView.openUserMarkupCollection(
				taggerView, userMarkupCollection);
		
	}
	
	public void analyze(Corpus corpus, IndexedRepository repository) {
		if (!analyzerManagerView.isAttached()) {
			menu.executeEntry(analyzerManagerView);
		}
		else {
			((Window)analyzerManagerView.getParent()).bringToFront();
		}
		analyzerManagerView.analyzeDocuments(corpus, repository);
	}
	
	public int addVisualization(Integer visualizationId, String caption,
			DistributionComputation distributionComputation, DistributionSelectionListener distributionSelectionListener) {
		if (!visualizationManagerView.isAttached()) {
			menu.executeEntry(visualizationManagerView);
		}
		else {
			((Window)visualizationManagerView.getParent()).bringToFront();
		}
		
		return visualizationManagerView.addVisualization(visualizationId, caption,
				distributionComputation, distributionSelectionListener);
	}

	@Override
	public void close() {
		repositoryManagerView.getRepositoryManager().close();
		logger.info("application for user" + getUser() + " has been closed");
		if (repositoryOpened) {
			userManager.logout(this);
			repositoryOpened = false;
		}
		super.close();
	}
	
	
	public void showAndLogError(String message, Throwable e) {
		logger.log(Level.SEVERE, "["+getUser()+"]" + message, e);
		
		if (message == null) {
			message = "internal error"; 
		}
		if (Page.getCurrent() != null) {
			HTMLNotification.show(
				"Error", 
				"An error has occurred!<br />" +
				"We've been notified about this error and it will be fixed soon.<br />" +
				"The underlying error message is:<br />" + message +
				"<br />" + e.getMessage(), 
				Type.ERROR_MESSAGE);
		}
	}

	public void openSourceDocument(SourceDocument sd, Repository repository,
			Range range) {
		TaggerView tv = openSourceDocument(sd, repository);
		tv.show(range);
	}

	public void addDoubleTree(List<KeywordInContext> kwics) {
		if (!visualizationManagerView.isAttached()) {
			menu.executeEntry(visualizationManagerView);
		}
		else {
			((Window)visualizationManagerView.getParent()).bringToFront();
		}

		visualizationManagerView.addDoubleTree(kwics);
	}

	@Override
	public Object getUser() {
		return user;
	}
	
	public void setUser(Object newUser) {
		Object currentUser = this.user;
		this.user = newUser;
		
		propertyChangeSupport.firePropertyChange(
				CatmaApplicationEvent.userChange.name(), currentUser, newUser);
	}
	
	@Override
	public void attach() {
		super.attach();
	}
	
	/**
	 * @see CatmaApplicationEvent
	 * @see PropertyChangeSupport#addPropertyChangeListener(String, PropertyChangeListener)
	 */
	public void addPropertyChangeListener(CatmaApplicationEvent propertyName,
			PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(propertyName.name(), listener);
	}

	/**
	 * @see CatmaApplicationEvent
	 * @see PropertyChangeSupport#removePropertyChangeListener(String, PropertyChangeListener)
	 */
	public void removePropertyChangeListener(CatmaApplicationEvent propertyName,
			PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName.name(),	listener);
	}

}
