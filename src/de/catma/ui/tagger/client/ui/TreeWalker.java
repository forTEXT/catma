package de.catma.ui.tagger.client.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.vaadin.terminal.gwt.client.VConsole;

public class TreeWalker {
	
    private Node startNode;
    private Node outerLeftNode;
    private Node outerRightNode;
    private boolean inAffectedSubtree = false;
    private boolean outOfAffectedSubtree = false;
    private Stack<Node> affectedNodes;
    private boolean isAfter;
    
	public TreeWalker(Element root, Node node1, Node node2) {
		super();
		affectedNodes = new Stack<Node>();
		VConsole.log("pos11");

		setStartNodeAndOuterLimits(root, node1, node2);
		
		walk(startNode);
//		DebugUtil.printNode(startNode);
//		DebugUtil.printNodes("affectedNodes", affectedNodes);
	}

	private void setStartNodeAndOuterLimits(Element root, Node node1, Node node2) {
		List<Node> node1Parents = getParents(root, node1);
		List<Node> node2Parents = getParents(root, node2);
		VConsole.log("pos12");

		if (!node1Parents.isEmpty() 
			&& !node2Parents.isEmpty() 
			&& (node1Parents.get(0).equals(node2Parents.get(0)))) {
			
			int idx = 0;
			int minParentIdx = Math.min(node1Parents.size(), node2Parents.size());
			VConsole.log("minParentIdx: " + minParentIdx);
			VConsole.log("pos12a");
			DebugUtil.printNodes("node1Parents", node1Parents);
			
			VConsole.log("pos12b");
			DebugUtil.printNodes("node2Parents", node2Parents);
			
			VConsole.log("pos12c");
			for (; idx<minParentIdx; idx++){
				VConsole.log("checking index: " + idx);
				if (!node1Parents.get(idx).equals(node2Parents.get(idx))) {
					break;
				}
			}
			VConsole.log("pos13");

			startNode = node1Parents.get(idx-1);
			VConsole.log("pos13a");
			if (idx == minParentIdx) { // one of the nodes has no text child so there is no such thing as "isAfter"
				isAfter = false;
				
				if (idx == node1Parents.size()) {
					outerLeftNode = node2;
					outerRightNode = node2;
				}
				else {
					outerLeftNode = node1;
					outerRightNode = node1;
				}
			}
			else {
				isAfter = 
					indexOf(startNode, node2Parents.get(idx)) 
						> indexOf(startNode, node1Parents.get(idx));

				if (isAfter) {
					outerLeftNode = node1;
					outerRightNode = node2;
				}
				else {
					outerLeftNode = node2;
					outerRightNode = node1;
				}
			}
			VConsole.log("pos14");
		
		}
	}
	
	private void walk(Node curNode) {
		if (!this.outOfAffectedSubtree 
				&& ((curNode.getNodeValue() == null) 
						|| !curNode.getNodeValue().trim().isEmpty())) {
			
			if (curNode.getNodeType() == Node.TEXT_NODE) {
				affectedNodes.push(curNode);
			}
			
			if (curNode.hasChildNodes()) {
				for (int i=0; i<curNode.getChildCount(); i++) {
					walk(curNode.getChild(i));
				}
			}
			else if(curNode.equals(outerLeftNode)) {
				this.inAffectedSubtree = true;
			}
			else if(curNode.equals(outerRightNode)) {
				this.outOfAffectedSubtree = true;
			}
			if (!inAffectedSubtree && (curNode.getNodeType() == Node.TEXT_NODE)) {
				affectedNodes.pop();
			}
		}
	}

	private List<Node> getParents(Element root, Node node) {
		ArrayList<Node> result = new ArrayList<Node>();
		while((node != null) && !node.equals(root)) {
			result.add(node);
			
			node = node.getParentNode();
		}
		Collections.reverse(result);
		return result;
	}
	
	private int indexOf(Node parent, Node child) {
		VConsole.log("pos13aa");
		for (int i=0; i<parent.getChildCount();i++) {
			VConsole.log("pos13ab");
			if (child.equals(parent.getChild(i))) {
				VConsole.log("pos13ac");
				return i;
			}
		}
		return -1; 
	}
    
	public List<Node> getAffectedNodes() {
		return affectedNodes;
	}

	public boolean isAfter() {
		return isAfter;
	}
}
