package de.catma.ui.client.ui.tagger.tagmanager;

import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SplitLayoutPanel;

import de.catma.ui.client.ui.tag.CTagsetDefinition;

public class TagManagerPanel extends Composite {
	
	private TagsetDefinitionsTreeModel curAvailableTagsetDefsTreeModel;
	private CellTree curAvailableTagsetDefsTree;
	
	public TagManagerPanel() {
		initComponents();
	}

	private void initComponents() {
//		SplitLayoutPanel splitLayout = new SplitLayoutPanel();
		
		curAvailableTagsetDefsTreeModel = new TagsetDefinitionsTreeModel();
		
		curAvailableTagsetDefsTree = 
				new CellTree(
						curAvailableTagsetDefsTreeModel, 
						null);
		
//		splitLayout.add(curAvailableTagsetDefsTree);
		initWidget(curAvailableTagsetDefsTree);
	}

	
	public void attachTagsetDefinition(CTagsetDefinition tagsetDefinition) {
		curAvailableTagsetDefsTreeModel.addTagsetDefinition(tagsetDefinition);
	}
	
}
