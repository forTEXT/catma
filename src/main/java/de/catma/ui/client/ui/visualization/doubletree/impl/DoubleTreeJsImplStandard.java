package de.catma.ui.client.ui.visualization.doubletree.impl;

import de.catma.ui.client.ui.visualization.doubletree.DoubleTreeJs;

public class DoubleTreeJsImplStandard {
	public native DoubleTreeJs create() /*-{
		var dt = new $wnd.doubletree.DoubleTree();
		return dt;
	}-*/;

	public native void init(DoubleTreeJs dt, String targetSelector) /*-{
		dt.init("div#"+targetSelector);
		
	}-*/;
	
	
	public native void visWidth(DoubleTreeJs dt, int width) /*-{
		dt.visWidth(width);
		dt.redraw();
	}-*/;
	
	public native void setupFromArrays(
			DoubleTreeJs dt, String[][] prefixArr, String[] tokenArr, String[][] postfixArr, boolean caseSensitive, boolean rightToLeftLanguage) /*-{
		var fieldNames = ["token"];
		var fieldDelim = "/";
		var distinguishingFieldsArray = ["token"];
		dt.setupFromArrays(
			prefixArr, tokenArr, postfixArr, null, caseSensitive, 
			fieldNames, fieldDelim, distinguishingFieldsArray, rightToLeftLanguage);	  
	}-*/;
	
}
