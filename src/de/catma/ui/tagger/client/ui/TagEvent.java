package de.catma.ui.tagger.client.ui;

import com.google.gwt.event.dom.client.DomEvent;

public class TagEvent extends DomEvent<TagEventHandler> {
	private static final String NAME = "tagevent";
	private static final Type<TagEventHandler> TYPE = new Type<TagEventHandler>(
			NAME, new TagEvent());

	protected TagEvent() {
		//TODO: params
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
		return toString(); // TODO: proper serialization
	}

}
