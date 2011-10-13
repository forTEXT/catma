package de.catma.ui.tagger.client.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.DomEvent;

import de.catma.ui.tagger.client.ui.shared.TaggedNode;


public class TagEvent extends DomEvent<TagEventHandler> {
	private static final String NAME = "tagevent";
	private static final Type<TagEventHandler> TYPE = new Type<TagEventHandler>(
			NAME, new TagEvent());
	private String tag;
	private List<TaggedNode> taggedNodes;

	TagEvent() {
		this("",new ArrayList<TaggedNode>());
	}
	
	public TagEvent(String tag, List<TaggedNode> taggedNodes) {
		this.tag=tag;
		this.taggedNodes=taggedNodes;
	}

	TagEvent(String tag, TaggedNode taggedNode) {
		this.tag = tag;
		taggedNodes = new ArrayList<TaggedNode>();
		taggedNodes.add(taggedNode);
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
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(tag);
		
		for (TaggedNode tn : taggedNodes) {
			builder.append(tn.toString());
		}
		
		return builder.toString(); 
	}

	@Override
	public String toDebugString() {
		return toString();
	}

	public String getTag() {
		return tag;
	}
}
