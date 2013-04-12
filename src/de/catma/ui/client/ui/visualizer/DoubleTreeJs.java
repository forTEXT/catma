package de.catma.ui.client.ui.visualizer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import de.catma.ui.client.ui.visualizer.impl.DoubleTreeJsImplStandard;

public final class DoubleTreeJs extends JavaScriptObject {
	
	protected DoubleTreeJs() {
	}

	private static DoubleTreeJsImplStandard impl = GWT.create(DoubleTreeJsImplStandard.class);
	
	public static DoubleTreeJs create() {
		return impl.create();
	}
	public void init(String targetSelector) {
		impl.init(this, targetSelector);
	}
	public void setupFromArrays(String[][] prefix, String[] tokens, String[][] postfix) {
		impl.setupFromArrays(this, prefix, tokens, postfix);
	}
}
