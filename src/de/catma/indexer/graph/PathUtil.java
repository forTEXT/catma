package de.catma.indexer.graph;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

class PathUtil {

	public Node getFirstPositionNode(Path p) {
		return getFirstNode(p, NodeType.Position);
	}
	
	public Node getFirstTermNode(Path p) {
		return getFirstNode(p, NodeType.Term);
	}
	
	private Node getFirstNode(Path p, NodeType type) {

		for (Node n : p.nodes()) {
			if (n.hasLabel(type)) {
				return n;
			}
		}
		throw new IllegalStateException("unable to detect first " + type.name() + " node!");
	}
}
