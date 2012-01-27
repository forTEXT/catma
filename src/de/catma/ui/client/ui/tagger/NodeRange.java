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

import com.google.gwt.dom.client.Node;

/**
 * @author marco.petris@web.de
 *
 */
public class NodeRange {

	private Node startNode;
	private int startOffset;
	private Node endNode;
	private int endOffset;

	public NodeRange() {
	}
	
	public NodeRange(
			Node startNode, int startOffset, 
			Node endNode, int endOffset) {
		super();
		this.startNode = startNode;
		this.startOffset = startOffset;
		this.endNode = endNode;
		this.endOffset = endOffset;
	}

	void addNode(Node node, int offset) {
		if (startNode == null) {
			startNode = node;
			startOffset = offset;
		}
		else if (endNode == null){
			endNode = node;
			endOffset = offset;
		}
	}

	public Node getStartNode() {
		return startNode;
	}

	public int getStartOffset() {
		return startOffset;
	}

	public Node getEndNode() {
		return endNode;
	}

	public int getEndOffset() {
		return endOffset;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("[");
		if (startNode == null) {
			builder.append("null,");
		}
		else {
			builder.append(startNode.getNodeValue().substring(0, Math.min(startNode.getNodeValue().length(), 10)));
			if(startNode.getNodeValue().length() > 10) {
				builder.append("...");
			}
			builder.append("|");
			builder.append(startOffset);
			builder.append(",");
		}
		if (endNode == null) {
			builder.append("null");
		}
		else {
			builder.append(endNode.getNodeValue().substring(0, Math.min(endNode.getNodeValue().length(), 10)));
			if(endNode.getNodeValue().length() > 10) {
				builder.append("...");
			}
			builder.append(endOffset);
			builder.append("]");
		}
		
		return builder.toString();
	}
	
}
