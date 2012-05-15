package de.catma;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.vaadin.Application;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.backgroundservice.ExecutionListener;
import de.catma.backgroundservice.ProgressCallable;
import de.catma.core.ExceptionHandler;
import de.catma.core.document.Corpus;
import de.catma.core.document.repository.Repository;
import de.catma.core.document.repository.RepositoryManager;
import de.catma.core.document.source.SourceDocument;
import de.catma.core.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagManager;
import de.catma.indexer.Indexer;
import de.catma.indexer.IndexerFactory;
import de.catma.indexer.IndexerProvider;
import de.catma.ui.DefaultProgressListener;
import de.catma.ui.ProgressWindow;
import de.catma.ui.analyzer.AnalyzerManagerView;
import de.catma.ui.analyzer.AnalyzerManagerWindow;
import de.catma.ui.analyzer.AnalyzerProvider;
import de.catma.ui.menu.Menu;
import de.catma.ui.menu.MenuFactory;
import de.catma.ui.repository.RepositoryManagerView;
import de.catma.ui.repository.RepositoryManagerWindow;
import de.catma.ui.tagger.TaggerManagerView;
import de.catma.ui.tagger.TaggerManagerWindow;
import de.catma.ui.tagger.TaggerView;
import de.catma.ui.tagmanager.TagManagerView;
import de.catma.ui.tagmanager.TagManagerWindow;

public class CleaApplication extends Application
	implements IndexerProvider, BackgroundServiceProvider, AnalyzerProvider {
	
	private static final String WEB_INF_DIR = "WEB-INF";
	private static final String CATMA_PROPERTY_FILE = "catma.properties";
	private enum PropertyKey {
		IndexerFactory,
		;
	}

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
	private Indexer indexer;
	private ProgressWindow progressWindow;

	@Override
	public void init() {
		
		Properties properties = loadProperties();
		backgroundService = new BackgroundService(this);
		
		
		final Window mainWindow = new Window("CATMA 4 - CLÉA");
		
		mainWindow.addListener(new CloseListener() {
			
			public void windowClose(CloseEvent e) {
				//TODO: close comm to elastic search
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
			
			createIndexer(properties);
			
			repositoryManagerView = 
				new RepositoryManagerView(
					new RepositoryManager(tagManager, properties));
		
			tagManagerView = new TagManagerView();
			
			taggerManagerView = new TaggerManagerView();
			
			analyzerManagerView = new AnalyzerManagerView();
			
			defaultProgressIndicator = new ProgressIndicator();
			defaultProgressIndicator.setIndeterminate(true);
			defaultProgressIndicator.setEnabled(false);
			defaultProgressIndicator.setPollingInterval(500);
			progressWindow = new ProgressWindow(defaultProgressIndicator);
//			mainLayout.addComponent(defaultProgressIndicator);
//			mainLayout.setComponentAlignment(
//					defaultProgressIndicator, Alignment.TOP_RIGHT);
			
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
							new AnalyzerManagerWindow(analyzerManagerView))
					);
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
//		LoginForm lf = new LoginForm();

		
		setMainWindow(mainWindow);
		
		setTheme("cleatheme");
	}
	
	//TODO: indexer should be provided by the repository!?
	private void createIndexer(Properties properties) 
			throws InstantiationException, IllegalAccessException, 
				ClassNotFoundException {
		String indexerFactoryClassName = 
				properties.getProperty(PropertyKey.IndexerFactory.name());
		IndexerFactory indexerFactory = 
				(IndexerFactory)Class.forName(indexerFactoryClassName).newInstance();
		this.indexer = indexerFactory.createIndexer();
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
		tagManagerView.openTagLibrary(tagManager, tagLibrary);
	}

	public TaggerView openSourceDocument(
			SourceDocument sourceDocument, Repository repository) {
		if (taggerManagerView.getApplication() == null) {
			menu.executeEntry(taggerManagerView);
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
	
	private void setDefaultProgressIndicatorEnabled(boolean enabled) {
		
		defaultProgressIndicator.setVisible(enabled);
		defaultProgressIndicator.setCaption("");
		
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
			final ProgressCallable<T> callable, 
			final ExecutionListener<T> listener) {
		setDefaultProgressIndicatorEnabled(true);
		
		defaultPIbackgroundJobs++;
		getBackgroundService().submit(
			callable, new ExecutionListener<T>() {
				public void done(T result) {
					listener.done(result);
					defaultPIbackgroundJobs--;
					if (defaultPIbackgroundJobs == 0) {
						setDefaultProgressIndicatorEnabled(false);
					}
					
				};
				
				public void error(Throwable t) {
					listener.error(t);
					defaultPIbackgroundJobs--;
					if (defaultPIbackgroundJobs == 0) {
						setDefaultProgressIndicatorEnabled(false);
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
	
	public void analyze(Corpus corpus, Repository repository) {
		if (analyzerManagerView.getApplication() == null) {
			menu.executeEntry(analyzerManagerView);
		}
		
		analyzerManagerView.analyzeDocuments(corpus, repository);
	}
	
	public Indexer getIndexer() {
		return this.indexer;
	}
}
