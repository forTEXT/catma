package de.catma.ui.analyzenew;

import com.vaadin.ui.ComponentContainer;

import de.catma.ui.dialog.AbstractOkCancelDialog;
import de.catma.ui.dialog.SaveCancelListener;

public class KwicVizPanel<T> extends AbstractOkCancelDialog<T>  implements VizPanel{

	public KwicVizPanel(String dialogCaption, SaveCancelListener<T> saveCancelListener) {
		super(dialogCaption, saveCancelListener);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void setQueryResults() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void addContent(ComponentContainer content) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected T getResult() {
		// TODO Auto-generated method stub
		return null;
	}

}
