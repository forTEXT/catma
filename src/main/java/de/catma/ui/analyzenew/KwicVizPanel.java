package de.catma.ui.analyzenew;

import com.vaadin.ui.ComponentContainer;

import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;

public class KwicVizPanel extends AbstractOkCancelDialog<VizSnapshot>  implements VizPanel{

	public KwicVizPanel(String dialogCaption, AnalyzeNewView analyzeNewView, SaveCancelListener<VizSnapshot> saveCancelListener) {
		super(dialogCaption, saveCancelListener);
		// TODO Auto-generated constructor stub
		
		//setParent(analyzeNewView);
	}

	@Override
	public void setQueryResults() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void addContent(ComponentContainer content) {
		// TODO Auto-generated method stub
		
	}
	public void attach() {
		super.attach();
//		((FocusHandler)UI.getCurrent()).focusDeferred(textInput);
	}
	@Override
	protected VizSnapshot getResult() {
		// TODO Auto-generated method stub
		return null;
	}

}
