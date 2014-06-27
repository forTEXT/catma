package de.catma.ui.client.ui.visualizer;

import java.util.Date;

import com.google.gwt.user.client.ui.Composite;

public class VDoubleTree extends Composite {
	
	private DoubleTreeWidget doubleTreeWidget;
	
	public VDoubleTree() {
		initComponents();
	}
	
    private void initComponents() {
    	doubleTreeWidget = new DoubleTreeWidget(new Date().getTime());
		initWidget(doubleTreeWidget);
	}

	public void updateTreeData(String treeData) {
		KwicList kwicList = KwicList.fromJSON(treeData);
			
			doubleTreeWidget.setupFromArrays(
				kwicList.getPrefixes(), kwicList.getTokens(), kwicList.getPostfixes(),
				kwicList.isCaseSensitive()); 
	}
	
	public void updateTreeWidth(String width) {
		doubleTreeWidget.setVisWidth(Integer.valueOf(width));
	}
}
