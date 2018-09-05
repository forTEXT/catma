package de.catma.ui.client.ui.visualizer.vega;

import java.util.Date;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.FocusWidget;

public class VegaWidget extends FocusWidget {
	
	public VegaWidget() {
		super(Document.get().createDivElement());
		
		getElement().setId("VegaWidget"+new Date().getTime());
	}

	public void setVegaSpec(String vegaSpec) {
		if ((vegaSpec != null) && !vegaSpec.trim().isEmpty()) {
			vegaEmbed(getElement().getId(), JsonUtils.safeEval(vegaSpec));
		}
	}
	
	public native void vegaEmbed(String elementId, JavaScriptObject vegaSpec) /*-{
		$wnd.vega.embed("#"+elementId, vegaSpec);
	}-*/;

}
