package de.catma.ui.module.analyze.visualization.doubletree;

import java.util.List;

import com.vaadin.ui.AbstractComponent;

import de.catma.document.source.KeywordInContext;
import de.catma.ui.client.ui.visualization.doubletree.DoubleTreeState;

public class DoubleTree extends AbstractComponent {

	private String kwicsJson;
	
	public DoubleTree() {
	}

	public void setupFromArrays(
			List<KeywordInContext> kwics,
			boolean caseSensitive) {
		kwicsJson = new KwicListJSONSerializer().toJSON(
				kwics, caseSensitive);
		getState().treeData = kwicsJson;
	}
	
	@Override
	protected DoubleTreeState getState() {
		return (DoubleTreeState) super.getState();
	}
}
