package de.catma.ui.analyzer;

import de.catma.ui.CatmaWindow;

public class VisualizationManagerWindow extends CatmaWindow {
	
	public VisualizationManagerWindow(VisualizationManagerView view) {
		super("Visualizer");
		setContent(view);
		setHeight("85%");
		setWidth("70%");
	}
}
