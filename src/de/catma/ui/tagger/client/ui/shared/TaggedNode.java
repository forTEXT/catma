package de.catma.ui.tagger.client.ui.shared;

import java.util.ArrayList;
import java.util.List;

public class TaggedNode {
	private String id;
	private int startOffset;
	private int endOffset;
	private String taggedSpanId;
	private int nodeIndex;
	
	public TaggedNode(String id, int nodeIndex, int endOffset, String taggedSpanId){
		this(id, nodeIndex, 0, endOffset, taggedSpanId);
	}

	public TaggedNode(String[] taggedNodeString) {
		this(
			taggedNodeString[0],
			Integer.valueOf(taggedNodeString[1]),
			Integer.valueOf(taggedNodeString[2]), 
			Integer.valueOf(taggedNodeString[3]),
			taggedNodeString[4]);
	}

	public TaggedNode(String id, int nodeIndex, int startOffset, int endOffset, String taggedSpanId) {
		this.id = id;
		this.nodeIndex = nodeIndex;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.taggedSpanId = taggedSpanId;		
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
	
	public int getNodeIndex() {
		return nodeIndex;
	}
	
	public String getTaggedSpanId() {
		return taggedSpanId;
	}
	
	@Override
	public String toString() {
		return "^[" + id + "," + nodeIndex + "," +startOffset + "," + endOffset + "," + taggedSpanId + "]";
	}

	public static List<TaggedNode> createTaggedNodes(String[] taggedNodesStringRepresentations) {
		List<TaggedNode> result = new ArrayList<TaggedNode>();
		for (String taggedNodeString : taggedNodesStringRepresentations) {
			result.add(new TaggedNode(taggedNodeString.replaceAll("[\\[\\]]", "").split(",")));
		}
		return result;
	}
}
