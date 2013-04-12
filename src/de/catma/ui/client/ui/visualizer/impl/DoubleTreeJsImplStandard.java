package de.catma.ui.client.ui.visualizer.impl;

import de.catma.ui.client.ui.visualizer.DoubleTreeJs;

public class DoubleTreeJsImplStandard {
	public native DoubleTreeJs create() /*-{
		var dt = new $wnd.doubletree.DoubleTree();
		return dt;
	}-*/;

	public native void init(DoubleTreeJs dt, String targetSelector) /*-{
		dt.init("div#"+targetSelector);
	}-*/;
	
	public native void setupFromArrays(
			DoubleTreeJs dt, String[][] prefixArr, String[] tokenArr, String[][] postfixArr) /*-{
		var caseSensitive = false;
		var fieldNames = ["token"];
		var fieldDelim = "/";
		var distinguishingFieldsArray = ["token"];
		dt.setupFromArrays(prefixArr, tokenArr, postfixArr, true, fieldNames, fieldDelim, distinguishingFieldsArray);	  
	}-*/;
	
}
