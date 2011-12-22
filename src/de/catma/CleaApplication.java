package de.catma;

import java.io.File;

import com.vaadin.Application;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.LoginForm;
import com.vaadin.ui.LoginForm.LoginEvent;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Window;

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

	@Override
	public void init() {
		final Window mainWindow = new Window("Clea");
		
		LoginForm lf = new LoginForm();
		
		
		Panel tagManagerPanel = new Panel("Tag Manager");
		
		Button tagGreen = new Button("green tag");
		Button tagLight_green = new Button("light_green tag");
		Button tagBlue = new Button("blue tag");
		Button tagRed = new Button("red tag");
		Panel editorPanel = new Panel("Tagger");
		final Tagger tagger = new Tagger();
		tagger.setSizeFull();
		editorPanel.getContent().setSizeUndefined();
		editorPanel.setWidth("640px");
		editorPanel.addComponent(tagger);
		editorPanel.setScrollable(true);
		tagGreen.addListener(new TagSelectionHandler("tag_green", tagger));
		tagLight_green.addListener(new TagSelectionHandler("tag_lightgreen", tagger));
		tagBlue.addListener(new TagSelectionHandler("tag_blue", tagger));
		tagRed.addListener(new TagSelectionHandler("tag_red", tagger));
		final HorizontalLayout mainLayout = new HorizontalLayout();
		SourceUploader sourceUploader = new SourceUploader(tagger);
		tagManagerPanel.addComponent(sourceUploader);
		tagManagerPanel.addComponent(tagGreen);
		tagManagerPanel.addComponent(tagLight_green);
		tagManagerPanel.addComponent(tagBlue);
		tagManagerPanel.addComponent(tagRed);
		
		//mainWindow.setContent(mainLayout);
		mainWindow.setContent(lf);
		
		lf.addListener(new LoginForm.LoginListener() {
			
			public void onLogin(LoginEvent event) {
				mainWindow.setContent(mainLayout);
				
				CleaApplication.this.setUser(event.getLoginParameter("username"));
				System.out.println(CleaApplication.this.getUser());
			}
		});
		
		mainLayout.addComponent(editorPanel);
		mainLayout.setExpandRatio(editorPanel, 2);
		mainLayout.addComponent(tagManagerPanel);
		mainLayout.setExpandRatio(tagManagerPanel, 1);
		
		setMainWindow(mainWindow);
		setTheme("cleatheme");
		File f = new File("nase.txt");
		System.out.println(f.getAbsolutePath());
		System.out.println(this.getContext().getBaseDirectory());
	}

}
