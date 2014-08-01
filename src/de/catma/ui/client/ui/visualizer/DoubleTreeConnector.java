package de.catma.ui.client.ui.visualizer;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.annotations.OnStateChange;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;

import de.catma.ui.visualizer.doubletree.DoubleTree;

@Connect(DoubleTree.class)
public class DoubleTreeConnector extends AbstractComponentConnector {

	@Override
	protected VDoubleTree createWidget() {
		return GWT.create(VDoubleTree.class);
	}

	@Override
	public DoubleTreeState getState() {
		return (DoubleTreeState) super.getState();
	}
	
	@OnStateChange("treeData")
	private void updateTreeData() {
		getWidget().updateTreeData(getState().treeData);
	}
	
	@Override
	public VDoubleTree getWidget() {
		return (VDoubleTree) super.getWidget();
	}
}
