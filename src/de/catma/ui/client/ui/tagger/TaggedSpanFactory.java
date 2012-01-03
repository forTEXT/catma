package de.catma.ui.client.ui.tagger;

import java.util.Date;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;

public class TaggedSpanFactory {

	private String tag;
	private String instanceID;
	private int instanceReferenceCounter = 1;
	
	public TaggedSpanFactory(String tag) {
		this(tag, String.valueOf(new Date().getTime()));
	}
	
	public TaggedSpanFactory(String tag, String instanceID) {
		super();
		this.tag = tag;
		this.instanceID = instanceID;
	}

	public Element createTaggedSpan(String innerHtml) {
		Element taggedSpan = DOM.createSpan();
		taggedSpan.addClassName(tag);
		taggedSpan.setId(instanceID + "_" + instanceReferenceCounter++);
		taggedSpan.setInnerHTML(innerHtml);
		return taggedSpan;
	}

	public String getTag() {
		return tag;
	}
	
	public String getInstanceID() {
		return instanceID;
	}
	
}
