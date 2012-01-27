/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2012  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */   
package de.catma.ui.client.ui.tagger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.vaadin.terminal.gwt.client.VConsole;

/**
 * @author marco.petris@web.de
 *
 */
public class AffectedNodesFinder {
	
    private Node startNode;
    private Node outerLeftNode;
    private Node outerRightNode;
    private boolean inAffectedSubtree = false;
    private boolean outOfAffectedSubtree = false;
    private Stack<Node> affectedNodes;
    private boolean isAfter;
    
	public AffectedNodesFinder(Element root, Node node1, Node node2) {
		super();
		affectedNodes = new Stack<Node>();
		
		setStartNodeAndOuterLimits(root, node1, node2);
		
		walk(startNode);
//		DebugUtil.printNode(startNode);
//		DebugUtil.printNodes("affectedNodes", affectedNodes);
	}

	private void setStartNodeAndOuterLimits(Element root, Node node1, Node node2) {
		List<Node> node1Parents = getParents(root, node1);
		List<Node> node2Parents = getParents(root, node2);
		
		if (!node1Parents.isEmpty() 
			&& !node2Parents.isEmpty() 
			&& (node1Parents.get(0).equals(node2Parents.get(0)))) {
			
			int idx = 0;
			
			// the smaller size of the two lists of parents is the maximum parent index 
			// we can check (for greater index numbers there is no partner parent to check against)
			int maxParentIdx = Math.min(node1Parents.size(), node2Parents.size());

			VConsole.log("maxParentIdx: " + maxParentIdx);

			DebugUtil.printNodes("node1Parents", node1Parents);
			DebugUtil.printNodes("node2Parents", node2Parents);
			
			// find the closest common parent node
			for (; idx<maxParentIdx; idx++){
				
				VConsole.log("searching for the closest common parent, checking index: " + idx);
				if (!node1Parents.get(idx).equals(node2Parents.get(idx))) {
					// ok, the last one was the closest, because now we are no longer on the common branch
					break;
				}
			}

			startNode = node1Parents.get(idx-1); // get the last common parent node, which is the closest

			// is one of the node a parent of the other?
			if (idx == maxParentIdx) {
				// one of the nodes is not a text node since text nodes cannot have children
				throw new IllegalStateException("idx==maxParentIdx, should not happen");
			}
			else {
				// if the second node's index is larger than the index of the first node
				// from their parents point of view, then the first node is on the outer left side of 
				// the subtree and the second node is on the outer right side of the subtree;
				// in the other case the positions are swapped 
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
		}
	}
	
	/**
	 * Recursively walks the tree depth first and creates a list of affected nodes.
	 * @param curNode the (relative) root node to start with 
	 */
	private void walk(Node curNode) {
		
		// check if we're still within the affected subtree 
		// and the current node has any taggable content
		if (!this.outOfAffectedSubtree 
				&& ((curNode.getNodeValue() == null) 
						|| !curNode.getNodeValue().trim().isEmpty())) {
			
			// all text nodes gets added, in case we get into the affected subtree with this 
			// node or one of its children 
			if (curNode.getNodeType() == Node.TEXT_NODE) {
				affectedNodes.push(curNode);
			}
			
			// we check for children and go down the subtrees
			if (curNode.hasChildNodes()) {
				for (int i=0; i<curNode.getChildCount(); i++) {
					walk(curNode.getChild(i));
				}
			}
			// if we reach the outer left node
			// we're in the affacted subtree -> all parent nodes can stay on the stack
			else if(curNode.equals(outerLeftNode)) {
				this.inAffectedSubtree = true;
			}
			// if we reach the outer right node
			// we reject all the rest of the upcoming nodes
			else if(curNode.equals(outerRightNode)) {
				this.outOfAffectedSubtree = true;
			}
			// if the current node is a text node it has already been pushed onto the stack
			// and if we're not within the affected subtree, we're removing the current node from the stack
			// (not being in the affected subtree means neither the current node nor one of its 
			//  children is the outer left node)
			if (!inAffectedSubtree && (curNode.getNodeType() == Node.TEXT_NODE)) {
				affectedNodes.pop();
			}
		}
	}

	/**
	 * @param root the root node 
	 * @param node the node to start from
	 * @return a list of nodes starting with the parent closest to the root
	 * 			 and ending with the given node
	 */
	private List<Node> getParents(Element root, Node node) {
		ArrayList<Node> result = new ArrayList<Node>();
		while((node != null) && !node.equals(root)) {
			result.add(node);
			
			node = node.getParentNode();
		}
		Collections.reverse(result);
		return result;
	}
	
	/**
	 * @param parent the parent node 
	 * @param child the child node
	 * @return the position of the child node within the array of children 
	 * 			or -1 if there is no parent child relationship
	 */
	public static int indexOf(Node parent, Node child) {
		for (int i=0; i<parent.getChildCount();i++) {
			if (child.equals(parent.getChild(i))) {
				return i;
			}
		}
		return -1; 
	}
    
	/**
	 * @return list of all affected nodes in a depth first order
	 */
	public List<Node> getAffectedNodes() {
		return affectedNodes;
	}

	/**
	 * @return <code>true</code>, if the nodes given to the TreeWalkers constructor are in the
	 * 		   right order within the subtree: node1 on the outer left and node2 on 
	 * 		   the outer right, <code>false</code> if it is the other way round
	 */
	public boolean isAfter() {
		return isAfter;
	}
}
