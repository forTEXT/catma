package de.catma.ui.tagger.client.ui.shared;

import java.util.ArrayList;
import java.util.List;

public class TaggedNode {
	private String id;
	private int startOffset;
	private int endOffset;
	private String taggedSpanId;
	
	public TaggedNode(String id, int endOffset, String taggedSpanId){
		this(id, 0, endOffset, taggedSpanId);
	}

	public TaggedNode(String id, int startOffset, int endOffset, String taggedSpanId) {
		super();
		this.id = id;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.taggedSpanId = taggedSpanId;
	}

	public TaggedNode(String[] taggedNodeString) {
		this(
			taggedNodeString[0],
			Integer.valueOf(taggedNodeString[1]), 
			Integer.valueOf(taggedNodeString[2]),
			taggedNodeString[3]);
	}

	public String getId() {
		return id;
	}

	public int getStartOffset() {
		return startOffset;
	}

	public int getEndOffset() {
		return endOffset;
	}
	
	@Override
	public String toString() {
		return "^[" + id + "," +startOffset + "," + endOffset + "," + taggedSpanId + "]";
	}

	public static List<TaggedNode> createTaggedNodes(String[] taggedNodesStringRepresentations) {
		List<TaggedNode> result = new ArrayList<TaggedNode>();
		for (String taggedNodeString : taggedNodesStringRepresentations) {
			result.add(new TaggedNode(taggedNodeString.replaceAll("[\\[\\]]", "").split(",")));
		}
		return result;
	}
}
