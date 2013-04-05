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
package de.catma;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vaadin.jouni.animator.Animator;

import com.vaadin.Application;
import com.vaadin.terminal.ClassResource;
import com.vaadin.terminal.ExternalResource;
import com.vaadin.terminal.ParameterHandler;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.Notification;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressCallable;
import de.catma.document.Corpus;
import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryManager;
import de.catma.document.repository.RepositoryPropertyKey;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.indexer.IndexedRepository;
import de.catma.queryengine.result.computation.DistributionComputation;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.ui.DefaultProgressListener;
import de.catma.ui.ProgressWindow;
import de.catma.ui.analyzer.AnalyzerManagerView;
import de.catma.ui.analyzer.AnalyzerManagerWindow;
import de.catma.ui.analyzer.AnalyzerProvider;
import de.catma.ui.analyzer.VisualizationManagerView;
import de.catma.ui.analyzer.VisualizationManagerWindow;
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

public class CatmaApplication extends Application
	implements BackgroundServiceProvider, AnalyzerProvider, ParameterHandler {
	
	private static final String VERSION = 
			"(v"+new SimpleDateFormat("yyyy/MM/dd-HH:mm").format(new Date())+")";
	private static final String WEB_INF_DIR = "WEB-INF";
	private static final String CATMA_PROPERTY_FILE = "catma.properties";

	private RepositoryManagerView repositoryManagerView;
	private TagManagerView tagManagerView;
	private Menu menu;
	private String tempDirectory = null;
	private BackgroundService backgroundService;
	private TaggerManagerView taggerManagerView;
	private AnalyzerManagerView analyzerManagerView;
	private TagManager tagManager;
	private ProgressIndicator defaultProgressIndicator;
	private int defaultPIbackgroundJobs = 0;
	private ProgressWindow progressWindow;
	private VisualizationManagerView visualizationManagerView;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Map<String,String[]> parameters = new HashMap<String, String[]>();

	@Override
	public void init() {
		
		Properties properties = loadProperties();
		backgroundService = new BackgroundService(this);
		
		
		final Window mainWindow = new Window("CATMA 4 - CLÉA " + VERSION);
		mainWindow.addParameterHandler(this);
		HorizontalLayout mainLayout = new HorizontalLayout();
		mainLayout.setSizeUndefined();
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);
		mainWindow.addStyleName("catma-mainwindow");
		
		mainWindow.setContent(mainLayout);
		MenuFactory menuFactory = new MenuFactory();
		try {
			initTempDirectory(properties);
			tagManager = new TagManager();
			
			repositoryManagerView = 
				new RepositoryManagerView(
					new RepositoryManager(
						this, tagManager, properties));
		
			tagManagerView = new TagManagerView(tagManager);
			
			taggerManagerView = new TaggerManagerView();
			
			analyzerManagerView = new AnalyzerManagerView();
			
			visualizationManagerView = new VisualizationManagerView();
			
			defaultProgressIndicator = new ProgressIndicator();
			defaultProgressIndicator.setIndeterminate(true);
			defaultProgressIndicator.setEnabled(false);
			defaultProgressIndicator.setPollingInterval(500);
			progressWindow = new ProgressWindow(defaultProgressIndicator);
			
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
			Link aboutLink = new Link(
					"About", new ExternalResource("http://www.catma.de"));
			aboutLink.setTargetName("_blank");
			mainLayout.addComponent(aboutLink);
			mainLayout.setComponentAlignment(aboutLink, Alignment.TOP_RIGHT);
			mainLayout.setExpandRatio(aboutLink, 1.0f);
			
			Link helpLink = new Link(
					"Help", new ExternalResource(getURL()+"manual/"));
			helpLink.setTargetName("_blank");
			mainLayout.addComponent(helpLink);
			mainLayout.setComponentAlignment(helpLink, Alignment.TOP_RIGHT);
			
			Label helpLabel = new Label();
			helpLabel.setIcon(new ClassResource(
					"ui/resources/icon-help.gif", 
					this));
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
			VerticalLayout helpWrapper = new VerticalLayout();
			helpWrapper.addComponent(helpLabel);
			helpWrapper.setComponentAlignment(helpLabel, Alignment.TOP_RIGHT);
			
			Animator helpAnimator = new Animator(helpWrapper);
			
			helpAnimator.setFadedOut(true);
			
			mainLayout.addComponent(helpAnimator);
			mainLayout.setComponentAlignment(helpAnimator, Alignment.TOP_RIGHT);
			helpAnimator.fadeIn(2000, 300);
			
			MenuBar loginLogoutMenu = new MenuBar();
			LoginLogoutCommand loginLogoutCommand = 
					new LoginLogoutCommand(menu, repositoryManagerView, this);
			MenuItem loginLogoutitem = loginLogoutMenu.addItem("Login", loginLogoutCommand);
			loginLogoutCommand.setLoginLogoutItem(loginLogoutitem);
			
			mainLayout.addComponent(loginLogoutMenu);
			mainLayout.setComponentAlignment(loginLogoutMenu, Alignment.TOP_RIGHT);
			mainLayout.setWidth("100%");
		} catch (Exception e) {
			showAndLogError("The system could not be initialized!", e);
		}

		setMainWindow(mainWindow);
		
		setTheme("cleatheme");
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
	private void initTempDirectory(Properties properties) throws IOException {
		String tempDirProp = properties.getProperty(RepositoryPropertyKey.TempDir.name());
		File tempDir = new File(tempDirProp);

		if (!tempDir.isAbsolute()) {
			this.tempDirectory = 
					this.getContext().getBaseDirectory() 
					+ System.getProperty("file.separator") 
					+ WEB_INF_DIR
					+ System.getProperty("file.separator")
					+ tempDirProp;
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

	private Properties loadProperties() {
		String path = 
				this.getContext().getBaseDirectory() 
				+ System.getProperty("file.separator") 
				+ CATMA_PROPERTY_FILE;
		
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(path));
		}
		catch( IOException e) {
			ExceptionHandler.log(e);
		}
		return properties;
	}

	public void openRepository(Repository repository) {
		repositoryManagerView.openRepository(repository);
	}
	 
	public void openTagLibrary(Repository repository, TagLibrary tagLibrary) {
		if (tagManagerView.getApplication() == null) {
			menu.executeEntry(tagManagerView);
		}
		else {
			tagManagerView.getWindow().bringToFront();
		}
		tagManagerView.openTagLibrary(repository, tagLibrary);
	}

	public TaggerView openSourceDocument(
			SourceDocument sourceDocument, Repository repository) {
		if (taggerManagerView.getApplication() == null) {
			menu.executeEntry(taggerManagerView);
			getMainWindow().showNotification(
					"Information", 
					"To markup your text please drag Tagsets from a Tag Library " +
					"into the currently active Tagsets area!",
					Notification.TYPE_TRAY_NOTIFICATION);
		}
		else {
			taggerManagerView.getWindow().bringToFront();
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
	
	private void setDefaultProgressIndicatorEnabled(
			String caption, boolean enabled) {
		
		defaultProgressIndicator.setVisible(enabled);
		defaultProgressIndicator.setCaption(caption);
		
		if (enabled) {
			if (progressWindow.getParent() == null) {
				getMainWindow().addWindow(progressWindow);
			}
		}
		else {
			if (progressWindow.getParent() != null) {
				getMainWindow().removeWindow(progressWindow);
			}
		}
		defaultProgressIndicator.setEnabled(enabled);
		
	}
	public <T> void submit( 
			String caption,
			final ProgressCallable<T> callable, 
			final ExecutionListener<T> listener) {
		setDefaultProgressIndicatorEnabled(caption, true);
		logger.info("submitting job '" + caption +  "' " + callable);
		defaultPIbackgroundJobs++;
		getBackgroundService().submit(
			callable, new ExecutionListener<T>() {
				public void done(T result) {
					listener.done(result);
					defaultPIbackgroundJobs--;
					if (defaultPIbackgroundJobs == 0) {
						setDefaultProgressIndicatorEnabled("", false);
					}
					
				};
				
				public void error(Throwable t) {
					listener.error(t);
					defaultPIbackgroundJobs--;
					if (defaultPIbackgroundJobs == 0) {
						setDefaultProgressIndicatorEnabled("", false);
					}
				}
			}, 
			new DefaultProgressListener(defaultProgressIndicator, this));
	}
	

	public void openUserMarkupCollection(
			SourceDocument sourceDocument, 
			UserMarkupCollection userMarkupCollection, Repository repository) {
		TaggerView taggerView = openSourceDocument(sourceDocument, repository);
		taggerManagerView.openUserMarkupCollection(
				taggerView, userMarkupCollection);
		
	}
	
	public void analyze(Corpus corpus, IndexedRepository repository) {
		if (analyzerManagerView.getApplication() == null) {
			menu.executeEntry(analyzerManagerView);
		}
		else {
			analyzerManagerView.getWindow().bringToFront();
		}
		analyzerManagerView.analyzeDocuments(corpus, repository);
	}
	
	public int addVisualization(Integer visualizationId, String caption,
			DistributionComputation distributionComputation) {
		if (visualizationManagerView.getApplication() == null) {
			menu.executeEntry(visualizationManagerView);
		}
		else {
			visualizationManagerView.getWindow().bringToFront();
		}
		
		return visualizationManagerView.addVisualization(visualizationId, caption,
				distributionComputation);
	}

	@Override
	public void close() {
		repositoryManagerView.getRepositoryManager().close();
		super.close();
	}
	
	
	public void showAndLogError(String message, Throwable e) {
		logger.log(Level.SEVERE, "["+getUser()+"]" + message, e);
		
		if (message == null) {
			message = "internal error"; 
		}
		
		getMainWindow().showNotification(
			"Error", 
			"An error has occurred!<br />" +
			"We've been notified about this error and it will be fixed soon.<br />" +
			"The underlying error message is:<br />" + message, 
			Notification.TYPE_ERROR_MESSAGE);
	}

	public void openSourceDocument(SourceDocument sd, Repository repository,
			Range range) {
		TaggerView tv = openSourceDocument(sd, repository);
		tv.show(range);
	}
	
}
