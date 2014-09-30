package de.catma.indexer.graph;

import org.neo4j.graphdb.Node;

class NodeTermInfo implements Comparable<NodeTermInfo> {

	private Node node;
	private int tokenOffset;

	public NodeTermInfo(int tokenOffset, Node node) {
		this.tokenOffset = tokenOffset;
		this.node = node;
	}

	public Node getNode() {
		return node;
	}
	
	@Override
	public int compareTo(NodeTermInfo o) {
		return this.tokenOffset-o.tokenOffset;
	}
}
