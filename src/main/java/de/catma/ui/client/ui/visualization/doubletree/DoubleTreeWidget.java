package de.catma.ui.client.ui.visualization.doubletree;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.FocusWidget;

public class DoubleTreeWidget extends FocusWidget {
	
	private DoubleTreeJs doubleTree;

	public DoubleTreeWidget(long treeId) {
		super(Document.get().createDivElement());

		getElement().setId("DoubleTreeWidget"+treeId);
	
		
		doubleTree = DoubleTreeJs.create();

	}
	
	public void setupFromArrays(
			String[][] prefix, 
			String[] tokens, 
			String[][] postfix, 
			boolean caseSensitive, boolean rightToLeftLanguage) {
		doubleTree.init(getElement().getId());
		doubleTree.setupFromArrays(prefix, tokens, postfix, caseSensitive, rightToLeftLanguage);
		((Element)getElement().getChild(0)).setAttribute("width", "100%");
		((Element)getElement().getChild(0)).setAttribute("height", "100%");
	}
}
