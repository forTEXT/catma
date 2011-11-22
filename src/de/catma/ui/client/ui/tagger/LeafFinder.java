package de.catma.ui.client.ui.tagger;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

public class LeafFinder {

	private Node nextLeftLeaf;
	private Node nextRightLeaf;
	private Node upperBorderNode;

	public LeafFinder(Node start) {
		this(start, Document.get());
	}
	
	public LeafFinder(Node start, Node upperBorderNode) {
		this.nextLeftLeaf = start;
		this.nextRightLeaf = start;
		this.upperBorderNode = upperBorderNode;
	}
	
	private void findNextRightLeaf() {
		if (this.nextRightLeaf != null) {
			Node candidate = this.nextRightLeaf.getNextSibling();
			if (candidate != null) {
				this.nextRightLeaf = descentRight(candidate);
			}
			else {
				Node nextParent = ascentRight(this.nextRightLeaf.getParentNode());
				if (nextParent == null) {
					this.nextRightLeaf = null;
				}
				else {
					this.nextRightLeaf = descentRight(nextParent);
				}
			}
		}
	}

	private void findNextLeftLeaf() {
		if (this.nextLeftLeaf != null) {
			Node candidate = this.nextLeftLeaf.getPreviousSibling();
			if (candidate != null) {
				this.nextLeftLeaf = descentLeft(candidate);
			}
			else {
				Node nextParent = ascentLeft(this.nextLeftLeaf.getParentNode());
				if (nextParent == null) {
					this.nextLeftLeaf = null;
				}
				else {
					this.nextLeftLeaf = descentLeft(nextParent);
				}
			}
		}
	}

	private Node ascentLeft(Node node) {
		if ((node != null) && (!node.equals(upperBorderNode))){
			Node sibling = node.getPreviousSibling();
			if (sibling == null) {
				return ascentLeft(node.getParentNode());
			}
			else {
				return sibling;
			}
		}
		return null;
	}

	private Node ascentRight(Node node) {
		if (node != null) {
			Node sibling = node.getNextSibling();
			if (sibling == null) {
				return ascentRight(node.getParentNode());
			}
			else {
				return sibling;
			}
		}
		return null;
	}
	
	private Node descentLeft(Node node) {
		if (node.hasChildNodes()) {
			return descentLeft(node.getChild(node.getChildCount()-1));
		}
		return node;
	}
	
	private Node descentRight(Node node) {
		if (node.hasChildNodes()) {
			return descentRight(node.getChild(0));
		}
		return node;
	}
	
	public Node getNextLeftLeaf() {
		findNextLeftLeaf();
		return this.nextLeftLeaf;
	}
	
	public Node getNextRigthLeaf() {
		findNextRightLeaf();
		return this.nextRightLeaf;
	}

	public Node getNextLeftTextLeaf() {
		do {
			getNextLeftLeaf();
		}
		while((this.nextLeftLeaf != null) && (Element.is(this.nextLeftLeaf)));
		
		return this.nextLeftLeaf;
	}
	
	public Node getNextRightTextLeaf() {
		do {
			getNextRigthLeaf();
		}
		while ((this.nextRightLeaf != null) && (Element.is(this.nextRightLeaf)));

		return this.nextRightLeaf;
	}

	public static Node getFirstTextLeaf(Node root) {
		
		if (root.hasChildNodes()) {
			Node candidate = root.getFirstChild();
			while(candidate.hasChildNodes()) {
				candidate = candidate.getFirstChild();
			}
			
			if (Element.is(candidate)) {
				LeafFinder leafFinder = new LeafFinder(candidate);
				return leafFinder.getNextRightTextLeaf();
			}
			else {
				return candidate;
			}
		}
		
		return null;
	}
}
