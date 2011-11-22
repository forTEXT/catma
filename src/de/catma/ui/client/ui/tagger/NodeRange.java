package de.catma.ui.client.ui.tagger;

import com.google.gwt.dom.client.Node;

public class NodeRange {

	private Node startNode;
	private int startOffset;
	private Node endNode;
	private int endOffset;

	public NodeRange() {
	}
	
	public NodeRange(
			Node startNode, int startOffset, 
			Node endNode, int endOffset) {
		super();
		this.startNode = startNode;
		this.startOffset = startOffset;
		this.endNode = endNode;
		this.endOffset = endOffset;
	}

	void addNode(Node node, int offset) {
		if (startNode == null) {
			startNode = node;
			startOffset = offset;
		}
		else if (endNode == null){
			endNode = node;
			endOffset = offset;
		}
	}

	public Node getStartNode() {
		return startNode;
	}

	public int getStartOffset() {
		return startOffset;
	}

	public Node getEndNode() {
		return endNode;
	}

	public int getEndOffset() {
		return endOffset;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("[");
		if (startNode == null) {
			builder.append("null,");
		}
		else {
			builder.append(startNode.getNodeValue().substring(0, Math.min(startNode.getNodeValue().length(), 10)));
			if(startNode.getNodeValue().length() > 10) {
				builder.append("...");
			}
			builder.append("|");
			builder.append(startOffset);
			builder.append(",");
		}
		if (endNode == null) {
			builder.append("null");
		}
		else {
			builder.append(endNode.getNodeValue().substring(0, Math.min(endNode.getNodeValue().length(), 10)));
			if(endNode.getNodeValue().length() > 10) {
				builder.append("...");
			}
			builder.append(endOffset);
			builder.append("]");
		}
		
		return builder.toString();
	}
	
}
