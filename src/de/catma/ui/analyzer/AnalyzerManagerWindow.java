package de.catma.ui.analyzer;

import de.catma.ui.CatmaWindow;

public class AnalyzerManagerWindow extends CatmaWindow {
	
	public AnalyzerManagerWindow(AnalyzerManagerView analyzerManagerView) {
		super("Analyzer");
		setContent(analyzerManagerView);
		setHeight("85%");
		setWidth("70%");
	}
}
