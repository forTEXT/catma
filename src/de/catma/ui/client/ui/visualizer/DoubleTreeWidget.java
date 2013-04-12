package de.catma.ui.client.ui.visualizer;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.FocusWidget;

public class DoubleTreeWidget extends FocusWidget {
	
	private DoubleTreeJs doubleTree;

	public DoubleTreeWidget(int treeId) {
		super(Document.get().createDivElement());

		getElement().setId("DoubleTreeWidget"+treeId);
		
		doubleTree = DoubleTreeJs.create();
	}
	
	public void setupFromArrays(String[][] prefix, String[] tokens, String[][] postfix) {
		doubleTree.init(getElement().getId());
		doubleTree.setupFromArrays(prefix, tokens, postfix);
	}
}
