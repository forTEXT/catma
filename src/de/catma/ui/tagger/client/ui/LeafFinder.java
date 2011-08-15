package de.catma.ui.tagger.client.ui;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

public class LeafFinder {

	private Node nextLeftLeaf;
	private Node nextRightLeaf;

	public LeafFinder(Node start) {
		this.nextLeftLeaf = start;
		this.nextRightLeaf = start;
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
		if (node != null) {
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
}
