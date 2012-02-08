package de.catma.ui.tagger;

import de.catma.ui.CatmaWindow;

public class TaggerManagerWindow extends CatmaWindow {
	
	public TaggerManagerWindow(TaggerManagerView taggerManagerView) {
		super("Tagger");
		setContent(taggerManagerView);
		setHeight("65%");
		setWidth("40%");
	}

}
