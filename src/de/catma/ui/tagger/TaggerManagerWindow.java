package de.catma.ui.tagger;

import de.catma.ui.CatmaWindow;

public class TaggerManagerWindow extends CatmaWindow {
	
	public TaggerManagerWindow(final TaggerManagerView taggerManagerView) {
		super("Tagger");
		setContent(taggerManagerView);
		setHeight("85%");
		setWidth("70%");
	}
}
