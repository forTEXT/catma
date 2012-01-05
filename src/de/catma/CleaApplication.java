package de.catma;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Properties;

import com.vaadin.Application;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import de.catma.core.ExceptionHandler;
import de.catma.core.document.repository.RepositoryManager;
import de.catma.ui.menu.MenuFactory;
import de.catma.ui.repository.RepositoryManagerView;
import de.catma.ui.repository.RepositoryManagerWindow;
import de.catma.ui.tagger.Tagger;

public class CleaApplication extends Application {
	
	private static class TagSelectionHandler implements ClickListener {
		
		private String tag;
		private Tagger tagger;

		public TagSelectionHandler(String tag, Tagger tagger) {
			super();
			this.tag = tag;
			this.tagger = tagger;
		}

		public void buttonClick(ClickEvent event) {
			tagger.addTag(tag);
			
		}
	}
	
	private static final String WEB_INF_DIR = "WEB-INF";
	private static final String CATMA_PROPERTY_FILE = "catma.properties";
	
	private RepositoryManagerView repositoryManagerView;

	@Override
	public void init() {
		
		Properties properties = loadProperties();
		
		
		final Window mainWindow = new Window("Clea");
		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setMargin(true);
		mainWindow.setContent(mainLayout);
		MenuFactory menuFactory = new MenuFactory();
		try {
			repositoryManagerView = 
					new RepositoryManagerView(new RepositoryManager(properties));
			menuFactory.createMenu(
					mainLayout, 
					new RepositoryManagerWindow(repositoryManagerView));
							
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
//		LoginForm lf = new LoginForm();
//		
//		
//		Panel tagManagerPanel = new Panel("Tag Manager");
//		
//		Button tagGreen = new Button("green tag");
//		Button tagLight_green = new Button("light_green tag");
//		Button tagBlue = new Button("blue tag");
//		Button tagRed = new Button("red tag");
//		Panel editorPanel = new Panel("Tagger");
//		final Tagger tagger = new Tagger();
//		tagger.setSizeFull();
//		editorPanel.getContent().setSizeUndefined();
//		editorPanel.setWidth("640px");
//		editorPanel.addComponent(tagger);
//		editorPanel.setScrollable(true);
//		tagGreen.addListener(new TagSelectionHandler("tag_green", tagger));
//		tagLight_green.addListener(new TagSelectionHandler("tag_lightgreen", tagger));
//		tagBlue.addListener(new TagSelectionHandler("tag_blue", tagger));
//		tagRed.addListener(new TagSelectionHandler("tag_red", tagger));
//		final HorizontalLayout mainLayout = new HorizontalLayout();
//		SourceUploader sourceUploader = new SourceUploader(tagger);
//		tagManagerPanel.addComponent(sourceUploader);
//		tagManagerPanel.addComponent(tagGreen);
//		tagManagerPanel.addComponent(tagLight_green);
//		tagManagerPanel.addComponent(tagBlue);
//		tagManagerPanel.addComponent(tagRed);
//		
//		//mainWindow.setContent(mainLayout);
//		mainWindow.setContent(lf);
//		
//		lf.addListener(new LoginForm.LoginListener() {
//			
//			public void onLogin(LoginEvent event) {
//				mainWindow.setContent(mainLayout);
//				
//				CleaApplication.this.setUser(event.getLoginParameter("username"));
//				System.out.println(CleaApplication.this.getUser());
//			}
//		});
//		
//		mainLayout.addComponent(editorPanel);
//		mainLayout.setExpandRatio(editorPanel, 2);
//		mainLayout.addComponent(tagManagerPanel);
//		mainLayout.setExpandRatio(tagManagerPanel, 1);
		
		setMainWindow(mainWindow);
		setTheme("cleatheme");

	}

	private Properties loadProperties() {
		String path = 
				this.getContext().getBaseDirectory() 
				+ System.getProperty("file.separator") 
				+ WEB_INF_DIR
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

	public RepositoryManagerView getRepositoryManagerView() {
		return repositoryManagerView;
	}
}
