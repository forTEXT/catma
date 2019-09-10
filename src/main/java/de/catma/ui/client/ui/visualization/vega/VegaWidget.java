package de.catma.ui.client.ui.visualization.vega;

import java.util.Date;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.FocusWidget;

public class VegaWidget extends FocusWidget {
	
	public VegaWidget() {
		super(Document.get().createDivElement());
		getElement().setId("VegaWidget"+new Date().getTime());
	}

	public void setVegaSpec(String vegaSpec, UserSelectionListener userSelectionListener) {
		if ((vegaSpec != null) && !vegaSpec.trim().isEmpty()) {
			vegaEmbed(
				getElement().getId(), 
				JsonUtils.safeEval(vegaSpec), 
				userSelectionListener);
		}
	}
	
	public native final void vegaEmbed(
			String elementId, 
			JavaScriptObject vegaSpec, 
			UserSelectionListener userSelectionListener) /*-{
		$wnd.vegaEmbed("#"+elementId, vegaSpec).then(function(result) {

			result.view.addSignalListener('userselection', function(name, value) {
			  userSelectionListener.@de.catma.ui.client.ui.visualization.vega.UserSelectionListener::userSelection(
			  	Lcom/google/gwt/core/client/JavaScriptObject;)(value);
			});
		});
	}-*/;

	public native final JsArray<JavaScriptObject> getUserSelectionArray(JavaScriptObject dataJson) /*-{
		return dataJson;
	}-*/;

	public native final int getStartOffset(JavaScriptObject row) /*-{
		return row.startOffset;
	}-*/;
	
	public native final int getEndOffset(JavaScriptObject row) /*-{
		return row.endOffset;
	}-*/;
	
	public native final String getSourceDocumentId(JavaScriptObject row) /*-{
		return row.sourceDocumentId;
	}-*/;

	public native final String getPhrase(JavaScriptObject row) /*-{
		return row.phrase;
	}-*/;

	public native final String getAnnotationCollectionId(JavaScriptObject row) /*-{
		return row.annotationCollectionId;
	}-*/;	

	public native final String getTagId(JavaScriptObject row) /*-{
		return row.tagId;
	}-*/;	

	public native final String getTagPath(JavaScriptObject row) /*-{
		return row.tagPath;
	}-*/;	

	public native final String getTagVersion(JavaScriptObject row) /*-{
		return row.tagVersion;
	}-*/;	
	
	public native final String getAnnotationId(JavaScriptObject row) /*-{
		return row.annotationId;
	}-*/;	
	
	public native final String getPropertyId(JavaScriptObject row) /*-{
		return row.propertyId;
	}-*/;	
	
	public native final String getPropertyName(JavaScriptObject row) /*-{
		return row.propertyName;
	}-*/;	

	public native final String getPropertyValue(JavaScriptObject row) /*-{
		return row.propertyValue;
	}-*/;

	public native final String getQueryId(JavaScriptObject row) /*-{
		return row.queryId;
	}-*/;
}
