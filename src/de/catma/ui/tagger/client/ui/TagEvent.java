package de.catma.ui.tagger.client.ui;

import com.google.gwt.event.dom.client.DomEvent;

public class TagEvent extends DomEvent<TagEventHandler> {
	private static final String NAME = "tagevent";
	private static final Type<TagEventHandler> TYPE = new Type<TagEventHandler>(
			NAME, new TagEvent());
	private String tag;

	protected TagEvent() {
		//TODO: params
	}
	
	TagEvent(String tag) {
		this.tag = tag;
	}
	
	@Override
	public final Type<TagEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(TagEventHandler handler) {
		handler.onTagEvent(this);
	}

	public static Type<TagEventHandler> getType() {
		return TYPE;
	}

	public static String getName() {
		return NAME;
	}
	
	public String toSerialization() {
		return tag; // TODO: proper serialization
	}

	public String getTag() {
		return tag;
	}
}
