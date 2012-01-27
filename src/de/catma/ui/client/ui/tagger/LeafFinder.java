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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

/**
 * @author marco.petris@web.de
 *
 */
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
