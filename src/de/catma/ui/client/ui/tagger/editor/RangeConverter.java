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
package de.catma.ui.client.ui.tagger.editor;

import java.util.logging.Logger;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import de.catma.ui.client.ui.tagger.DebugUtil;
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

	private static Logger logger = Logger.getLogger(RangeConverter.class.getName());
	
	private String taggerID;

	
	public RangeConverter(String taggerID) {
		this.taggerID = taggerID;
	}

	public TextRange convertToTextRange(Range range) {
		return convertToTextRange(
			range.getStartNode(), range.getEndNode(), 
			range.getStartOffset(), range.getEndOffset());
	}
	
	public TextRange convertToTextRange(
			Node startNode, Node endNode, int startOffset, int endOffset) {
		Node root = Document.get().getElementById(
				ContentElementID.CONTENT.name() + taggerID);
		
		int startPos = findPos(startNode, root)+startOffset;
		int endPos = findPos(endNode, root)+endOffset;
		TextRange tr = new TextRange(startPos, endPos);
		logger.info("RANGE!: " + tr);
		
		return tr;
	}
	
	public NodeRange convertToNodeRange(TextRange textRange) {
		Node root = Document.get().getElementById(
				ContentElementID.CONTENT.name() + taggerID);
		
		Node textLeaf = LeafFinder.getFirstTextLeaf(root);
		//TODO: should not return null, but better handle this case here and elsewhere in this class 
		logger.info("converting Text- to NodeRange starting at first text leaf: " + DebugUtil.getNodeInfo(textLeaf));
		
		LeafFinder leafFinder = new LeafFinder(textLeaf,root);
		NodeRange nodeRange = new NodeRange();
		int lastSize = findAndAddNode(0, leafFinder, textLeaf, textRange.getStartPos(), nodeRange);
		
		findAndAddNode(lastSize, leafFinder, nodeRange.getStartNode(), textRange.getEndPos(), nodeRange);
		
		logger.info("STARTNODE!: " + nodeRange.getStartNode().getNodeValue());
		logger.info("STARTPOS!: " + nodeRange.getStartOffset());
		logger.info("ENDNODE!: " + nodeRange.getEndNode().getNodeValue());
		logger.info("ENDPOS!: " + nodeRange.getEndOffset());
		
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
			logger.info("startOffset: " + startOffset);
			
			DebugUtil.printNode(endNode);
			logger.info("endOffset: " + endOffset);

			
			if (taggerElement.isOrHasChild(endNode) 
					&& taggerElement.isOrHasChild(startNode)) {

				if (Element.is(startNode)) {
					startNode = findClosestTextNode(startNode.getChild(startOffset),Direction.RIGHT);
					logger.info("Found closest text node for startNode: ");
					DebugUtil.printNode(startNode);
					startOffset = 0;
				}
	
				if (Element.is(endNode)) {
					endNode = findClosestTextNode(endNode.getChild(endOffset), Direction.LEFT);
					logger.info("Found closest text node for endNode: ");
					DebugUtil.printNode(endNode);
					endOffset = endNode.getNodeValue().length();
				}
				nodeRange = new NodeRange(startNode, startOffset, endNode, endOffset);
			}
			else {
				logger.info("at least one node is out of the tagger's bounds");
			}
		}
		else {
			logger.info("range is empty or out of the tagger's bounds");
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
	
	/**
	 * Hopping forward (to the right) through the text leaves until the text leaf
	 * with the position pos has been found. Startposition is startPos and 
	 * startNode is textLeaf
	 * @param startPos start position
	 * @param leafFinder the finder for text leaves
	 * @param textLeaf start node
	 * @param pos the position we are looking for
	 * @param nodeRange the node container for the result
	 * @return the endposition, where the search stopped
	 */
	private int findAndAddNode(
			int startPos, LeafFinder leafFinder, Node textLeaf, 
			int pos, NodeRange nodeRange) {
		logger.info(
			"looking for position " + pos 
			+ " from startpos " + pos + "@" + DebugUtil.getNodeInfo(textLeaf));
		
		int curSize = startPos;
		Node node = null;
		
		while((textLeaf != null) && (node == null)){
			if ((curSize + textLeaf.getNodeValue().length()) >= pos) {
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
