package de.catma.ui.client.ui.tagger.editor;

import com.google.gwt.dom.client.Element;

public abstract class SpanFactory {

	private String instanceID;
	protected int instanceReferenceCounter = 1;

	public SpanFactory() {
		this(IDGenerator.generate());
	}

	public SpanFactory(String instanceID) {
		this.instanceID = instanceID;
	}
	
	public String getInstanceID() {
		return instanceID;
	}
	
	public abstract Element createTaggedSpan(String innerHtml);
}
