package de.catma;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.Application;
import com.vaadin.service.ApplicationContext;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;
import com.vaadin.ui.Window.Notification;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressCallable;
import de.catma.document.Corpus;
import de.catma.document.Range;
import de.catma.document.repository.Repository;
import de.catma.document.repository.RepositoryManager;
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
	implements BackgroundServiceProvider, AnalyzerProvider {
	
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

	@Override
	public void init() {
		
		Properties properties = loadProperties();
		backgroundService = new BackgroundService(this);
		
		
		final Window mainWindow = new Window("CATMA 4 - CLÉA");
		
		mainWindow.addListener(new CloseListener() {
			
			public void windowClose(CloseEvent e) {
				// TODO: what should we do when the user closes (or reloads) the browser window
				// could be:
				// close the application -> not so good on reloads
				// leave it open -> then we need another way of closing the app (logout button?!)
			}
			
		});
		
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setSizeUndefined();
		mainLayout.setMargin(true);
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
		
		} catch (Exception e) {
			showAndLogError("The system could not be initialized!", e);
		}

		setMainWindow(mainWindow);
		
		setTheme("cleatheme");
	}
	
	@Override
	public void start(URL applicationUrl, Properties applicationProperties,
			ApplicationContext context) {
		System.out.println("Starting: " + applicationUrl );
		super.start(applicationUrl, applicationProperties, context);
	}
	
	//TODO: temp dir might be obsolete, better work with in-memory byte arrays
	private void initTempDirectory(Properties properties) throws IOException {
		String tempDirProp = properties.getProperty("TempDir");
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
			throw new IOException("could not create temporary directory: " + this.tempDirectory);
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
	 
	public void openTagLibrary(TagLibrary tagLibrary) {
		if (tagManagerView.getApplication() == null) {
			menu.executeEntry(tagManagerView);
		}
		else {
			tagManagerView.getWindow().bringToFront();
		}
		tagManagerView.openTagLibrary(tagLibrary);
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
				tagManager, sourceDocument, repository);
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
		logger.log(Level.SEVERE, message, e);
		
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
