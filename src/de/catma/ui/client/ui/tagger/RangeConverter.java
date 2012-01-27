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
import com.vaadin.terminal.gwt.client.VConsole;

import de.catma.ui.client.ui.tagger.impl.SelectionHandlerImplStandard.Range;
import de.catma.ui.client.ui.tagger.shared.ContentElementID;
import de.catma.ui.client.ui.tagger.shared.TextRange;

/**
 * @author marco.petris@web.de
 *
 */
public class RangeConverter {
	private static enum Direction {
		RIGHT,
		LEFT;
	}

	
	public TextRange convertToTextRange(Range range) {
		Node root = Document.get().getElementById(ContentElementID.CONTENT.name());
		
		int startPos = findPos(range.getStartNode(), root)+range.getStartOffset();
		int endPos = findPos(range.getEndNode(), root)+range.getEndOffset();
		TextRange tr = new TextRange(startPos, endPos);
		VConsole.log("RANGE!: " + tr);
		
		return tr;
	}
	
	public NodeRange convertToNodeRange(TextRange textRange) {
		Node root = Document.get().getElementById(ContentElementID.CONTENT.name());
		
		Node textLeaf = LeafFinder.getFirstTextLeaf(root);
		//TODO: should not return null, but better handle this case here and elsewhere in this class 
		
		LeafFinder leafFinder = new LeafFinder(textLeaf,root);
		NodeRange nodeRange = new NodeRange();
		int lastSize = findAndAddNode(0, leafFinder, textLeaf, textRange.getStartPos(), nodeRange);
		findAndAddNode(lastSize, leafFinder, nodeRange.getStartNode(), textRange.getEndPos(), nodeRange);
		
		VConsole.log("STARTNODE!: " + nodeRange.getStartNode().getNodeValue());
		VConsole.log("STARTPOS!: " + nodeRange.getStartOffset());
		VConsole.log("ENDNODE!: " + nodeRange.getEndNode().getNodeValue());
		VConsole.log("ENDPOS!: " + nodeRange.getEndOffset());
		
		return nodeRange;
	}

	public NodeRange convertToNodeRange(Element taggerElement, Range range) {
		NodeRange nodeRange = null;
		if ((range!= null) && (!range.isEmpty())) {
			
			Node startNode = range.getStartNode();
			int startOffset = range.getStartOffset();
			
			Node endNode = range.getEndNode();
			int endOffset = range.getEndOffset();
			
			DebugUtil.printNode(startNode);
			VConsole.log("startOffset: " + startOffset);
			
			DebugUtil.printNode(endNode);
			VConsole.log("endOffset: " + endOffset);

			
			if (taggerElement.isOrHasChild(endNode) 
					&& taggerElement.isOrHasChild(startNode)) {

				if (Element.is(startNode)) {
					startNode = findClosestTextNode(startNode.getChild(startOffset),Direction.RIGHT);
					VConsole.log("Found closest text node for startNode: ");
					DebugUtil.printNode(startNode);
					startOffset = 0;
				}
	
				if (Element.is(endNode)) {
					endNode = findClosestTextNode(endNode.getChild(endOffset), Direction.LEFT);
					VConsole.log("Found closest text node for endNode: ");
					DebugUtil.printNode(endNode);
					endOffset = endNode.getNodeValue().length();
				}
				nodeRange = new NodeRange(startNode, startOffset, endNode, endOffset);
			}
			else {
				VConsole.log("at least one node is out of the tagger's bounds");
			}
		}
		else {
			VConsole.log("range is empty or out of the tagger's bounds");
		}
		
		return nodeRange;
	}
	
	private int findPos(Node node, Node root) {
		int pos = 0;
		
		LeafFinder leafFinder = new LeafFinder(node, root);
		
		Node leftLeaf = leafFinder.getNextLeftTextLeaf();
		
		while(leftLeaf != null) {
			pos += leftLeaf.getNodeValue().length();
			leftLeaf = leafFinder.getNextLeftTextLeaf();
		}
		
		return pos;
	}
	
	private int findAndAddNode(
			int startSize, LeafFinder leafFinder, Node textLeaf, 
			int pos, NodeRange nodeRange) {
		
		int curSize = startSize;
		Node node = null;
		
		while((textLeaf != null) && (node == null)){
			if ((curSize + textLeaf.getNodeValue().length()) > pos) {
				node = textLeaf;
			}
			else {
				curSize += textLeaf.getNodeValue().length();
				textLeaf = leafFinder.getNextRightTextLeaf();
			}
		}

		nodeRange.addNode(textLeaf, pos-curSize);

		return curSize;
	}
	
	private Node findClosestTextNode(Node node, Direction direction) {
		if (direction == Direction.RIGHT) {
			LeafFinder leftLeafWalker = new LeafFinder(node);
			return leftLeafWalker.getNextRightTextLeaf();
		}
		else {
			LeafFinder leftLeafWalker = new LeafFinder(node);
			return leftLeafWalker.getNextLeftTextLeaf();
		}
	}
	

}
