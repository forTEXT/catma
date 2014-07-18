package de.catma.ui.visualizer.doubletree;

import java.util.List;

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.UI;

import de.catma.CatmaApplication;
import de.catma.document.source.KeywordInContext;
import de.catma.ui.client.ui.visualizer.DoubleTreeState;
import de.catma.ui.data.util.JSONSerializationException;

public class DoubleTree extends AbstractComponent {

	private String kwicsJson;
	
	public DoubleTree() {
	}

	public void setupFromArrays(List<KeywordInContext> kwics, boolean caseSensitive) {
		try {
			kwicsJson = new KwicListJSONSerializer().toJSON(kwics, caseSensitive);
			getState().treeData = kwicsJson;
		} catch (JSONSerializationException e) {
			((CatmaApplication)UI.getCurrent()).showAndLogError(
					"Error showing DoubleTree in the Visualizer!", e);
		}	
	}
	
	public void setVisWidth(int width) {
//		getState().treeWidth = width;
	}
	
	@Override
	protected DoubleTreeState getState() {
		return (DoubleTreeState) super.getState();
	}
}
