package de.catma.indexer.graph;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

class PathUtil {

	/**
	 * @param p
	 * @param max
	 * @return the Position node by going backwards positionNodeHops Position nodes
	 */
	public Node getPositionNodeBackwardsAt(Path p, int positionNodeHops) {
		int pCount = 0;
		for (Node n : p.reverseNodes()) {
			if (n.hasLabel(NodeType.Position)) {
				pCount++;
			}
			
			if (pCount==positionNodeHops) {
				return n;
			}
			
		}
		throw new IllegalStateException(
			"unable to detect first Position node within " 
					+ positionNodeHops + " hops window!");
	}
	
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
