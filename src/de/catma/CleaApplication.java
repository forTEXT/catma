package de.catma;

import com.vaadin.Application;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
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
		Window mainWindow = new Window("Clea");
		Panel p = new Panel("Tag Manager");
		
		final Tagger t = new Tagger();
		SourceUploader b = new SourceUploader(t);
		mainWindow.addComponent(t);
		mainWindow.addComponent(b);
		setMainWindow(mainWindow);
		setTheme("cleatheme");
	}

}
