package de.catma.ui.visualizer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.terminal.PaintException;
import com.vaadin.terminal.PaintTarget;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.ClientWidget;

import de.catma.CatmaApplication;
import de.catma.document.source.KeywordInContext;
import de.catma.ui.client.ui.visualizer.VDoubleTree;
import de.catma.ui.client.ui.visualizer.shared.DoubleTreeMessageAttribute;
import de.catma.ui.data.util.JSONSerializationException;

@ClientWidget(VDoubleTree.class)
public class DoubleTree extends AbstractComponent {

	private Map<String,String> attributes = new HashMap<String, String>();

	public DoubleTree() {
	}
	
	@Override
	public void paintContent(PaintTarget target) throws PaintException {
		super.paintContent(target);
		
		for (Map.Entry<String, String> entry : attributes.entrySet()) {
			target.addAttribute(entry.getKey(), entry.getValue());
		}
		
		attributes.clear();

	}
	
	@Override
	public void changeVariables(Object source, Map<String, Object> variables) {
		super.changeVariables(source, variables);
	}
	
	public void setupFromArrays(List<KeywordInContext> kwics) {
		try {
			String kwicsJson = new KwicListJSONSerializer().toJSON(kwics);
			System.out.println(kwicsJson);
			attributes.put(
					DoubleTreeMessageAttribute.SET.name(), 
					kwicsJson);
			requestRepaint();
		} catch (JSONSerializationException e) {
			((CatmaApplication)getApplication()).showAndLogError(
					"Error showing DoubleTree in the Visualizer!", e);
		}	
	}
}
