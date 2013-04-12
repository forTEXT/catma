package de.catma.ui.visualizer;

import java.util.List;

import com.vaadin.ui.HorizontalLayout;

import de.catma.document.source.KeywordInContext;
import de.catma.ui.tabbedview.ClosableTab;

public class DoubleTreeView  extends HorizontalLayout implements ClosableTab {
	
	
	private DoubleTree doubleTree;

	public DoubleTreeView(List<KeywordInContext> kwics) {
		initComponents();
		
		doubleTree.setupFromArrays(kwics);
	}

	private void initComponents() {
		doubleTree = new DoubleTree();
		addComponent(doubleTree);
	}

	public void close() { /* noop */ }

	public void addClickshortCuts() { /* noop */ }
	
	public void removeClickshortCuts() { /* noop */ }	
	
	@Override
	public String toString() {
		return "KWIC as a DoubleTree";
	}
}
