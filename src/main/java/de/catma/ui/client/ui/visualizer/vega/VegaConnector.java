package de.catma.ui.client.ui.visualizer.vega;

import java.util.ArrayList;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.annotations.OnStateChange;
import com.vaadin.client.communication.RpcProxy;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;

import de.catma.ui.client.ui.visualizer.vega.shared.SelectedQueryResultRow;
import de.catma.ui.visualizer.vega.Vega;

@Connect(Vega.class)
public class VegaConnector extends AbstractComponentConnector {
	
	private VegaServerRpc vegaServerRpc = RpcProxy.create(VegaServerRpc.class, this);
	
	@Override
	protected Widget createWidget() {
		return GWT.create(VegaWidget.class);
	}

	@Override
	public VegaWidget getWidget() {
		return (VegaWidget) super.getWidget();
	}

	@Override
	public VegaState getState() {
		return (VegaState)super.getState();
	}
	
	@OnStateChange("vegaSpec")
	private void setVegaSpec() {
		getWidget().setVegaSpec(getState().vegaSpec, new UserSelectionListener() {

			@Override
			public void userSelection(JavaScriptObject dataJson) {
				JsArray<JavaScriptObject> rows = getWidget().getUserSelectionArray(dataJson);
				ArrayList<SelectedQueryResultRow> result = new ArrayList<SelectedQueryResultRow>();
				
				for (int i=0; i<rows.length(); i++) {
					JavaScriptObject row = rows.get(i);
					
					SelectedQueryResultRow selectedQueryResultRow = new SelectedQueryResultRow();
					
					selectedQueryResultRow.setSourceDocumentId(getWidget().getSourceDocumentId(row));
					selectedQueryResultRow.setStartOffset(getWidget().getStartOffset(row));
					selectedQueryResultRow.setEndOffset(getWidget().getEndOffset(row));
					selectedQueryResultRow.setPhrase(getWidget().getPhrase(row));
					selectedQueryResultRow.setAnnotationCollectionId(getWidget().getAnnotationCollectionId(row));
					selectedQueryResultRow.setTagId(getWidget().getTagId(row));
					selectedQueryResultRow.setTagPath(getWidget().getTagPath(row));
					selectedQueryResultRow.setTagVersion(getWidget().getTagVersion(row));
					selectedQueryResultRow.setAnnotationId(getWidget().getAnnotationId(row));
					selectedQueryResultRow.setPropertyId(getWidget().getPropertyId(row));
					selectedQueryResultRow.setPropertyName(getWidget().getPropertyName(row));
					selectedQueryResultRow.setPropertyValue(getWidget().getPropertyValue(row));
					result.add(selectedQueryResultRow);
				}
			
				vegaServerRpc.onUserSelection(result);
			}
		});
	}
}
