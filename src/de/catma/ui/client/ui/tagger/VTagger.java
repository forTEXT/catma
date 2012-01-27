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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Text;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.VConsole;
import com.vaadin.terminal.gwt.client.ValueMap;

import de.catma.ui.client.ui.tagger.impl.SelectionHandlerImplStandard;
import de.catma.ui.client.ui.tagger.impl.SelectionHandlerImplStandard.Range;
import de.catma.ui.client.ui.tagger.menu.TagMenu;
import de.catma.ui.client.ui.tagger.shared.EventAttribute;
import de.catma.ui.client.ui.tagger.shared.TagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;


/**
 * @author marco.petris@web.de
 *
 */
public class VTagger extends FocusWidget implements Paintable, MouseUpHandler {

	/** Set the CSS class name to allow styling. */
	public static final String TAGGER_STYLE_CLASS = "v-tagger";

	private static SelectionHandlerImplStandard impl = 
			 GWT.create(SelectionHandlerImplStandard.class);

	private List<Range> lastRangeList; 

	/** The client side widget identifier */
	private String clientID;

	/** Reference to the server connection object. */
	private ApplicationConnection serverConnection;
	
	private HashMap<String, TagInstance> tagInstances = new HashMap<String, TagInstance>();
	
	public VTagger() {
		this(Document.get().createDivElement());
	}
	
	/**
	 * The constructor should first call super() to initialize the component and
	 * then handle any initialization relevant to Vaadin.
	 */
	public VTagger(Element element) {
		super(element);
		getElement().setId("vtagger-element");

		//setStyleName(TAGGER_STYLE_CLASS);
		setStylePrimaryName(TAGGER_STYLE_CLASS);
		
		// Tell GWT we are interested in consuming click events
		sinkEvents(Event.ONMOUSEUP);

		addMouseUpHandler(this);

		addMouseMoveHandler(new TagMenu(this));
	}
	
	public void removeTag(String tagInstanceID) {
		removeTag(tagInstanceID, true);
	}
	public void removeTag(String tagInstanceID, boolean reportToServer) {
		int currentPartID = 1;
		Element taggedSpan = Document.get().getElementById(tagInstanceID + "_" + currentPartID++);
		while(taggedSpan != null) {
			Element parent = taggedSpan.getParentElement();
			DebugUtil.printNode(taggedSpan);
			NodeList<Node> children = taggedSpan.getChildNodes();
			for (int i=0; i<children.getLength(); i++) {
				Node child = children.getItem(i);
				parent.insertBefore(child.cloneNode(true), taggedSpan);
				
			}
			parent.removeChild(taggedSpan);
			taggedSpan = Document.get().getElementById(tagInstanceID + "_" + currentPartID++);
		}
		tagInstances.remove(tagInstanceID);
		if (reportToServer) {
			sendMessage(EventAttribute.TAGINSTANCE_REMOVE, tagInstanceID);
		}
	}

	public void onMouseUp(MouseUpEvent event) {
		lastRangeList = impl.getRangeList();
		VConsole.log("Ranges: " + lastRangeList.size());
	}
	
    /**
     * Called whenever an update is received from the server 
     */
	public void updateFromUIDL(UIDL uidl, ApplicationConnection client) {
		// This call should be made first. 
		// It handles sizes, captions, tooltips, etc. automatically.
		if (client.updateComponent(this, uidl, true)) {
		    // If client.updateComponent returns true there has been no changes and we
		    // do not need to update anything.
			return;
		}

		// Save reference to server connection object to be able to send
		// user interaction later
		this.serverConnection = client;

		// Save the client side identifier (paintable id) for the widget
		this.clientID = uidl.getId();

		if (uidl.hasAttribute(EventAttribute.HTML.name())) {
			VConsole.log("setting html");
			setHTML(new HTML(uidl.getStringAttribute(EventAttribute.HTML.name())));
		}


		if (uidl.hasAttribute(EventAttribute.TAGINSTANCE_CLEAR.name()) 
				&& uidl.getBooleanAttribute(EventAttribute.TAGINSTANCE_CLEAR.name())) {
			ArrayList<String> keyCopy = new ArrayList<String>();
			keyCopy.addAll(tagInstances.keySet());
			for (String tagInstanceID : keyCopy) {
				removeTag(tagInstanceID, false);
			}
		}
		
		int i=0;
		RangeConverter rangeConverter = new RangeConverter();

		while (uidl.hasAttribute(EventAttribute.TAGINSTANCE.name()+i)) {
			ValueMap tagInstanceValueMap = 
					uidl.getMapAttribute(EventAttribute.TAGINSTANCE.name()+i);
			TagInstance tagInstance = getTagInstance(tagInstanceValueMap);
			VConsole.log("got tag instance from server: " + tagInstance);
			
			tagInstances.put(tagInstance.getInstanceID(), tagInstance);
			
			TaggedSpanFactory taggedSpanFactory = 
					new TaggedSpanFactory(tagInstance.getInstanceID(), tagInstance.getColor());
			for (TextRange textRange : tagInstance.getRanges()) {
				addTagToRange(taggedSpanFactory, rangeConverter.convertToNodeRange(textRange));
			}
			
			i++;
		}
	}

 	public void setHTML(HTML html) {
 		html.getElement().setId("vtagger-html-div");
 		
		if (getElement().hasChildNodes()) {
			NodeList<Node> children = getElement().getChildNodes();
			for (int i=0; i<children.getLength();i++) {
				getElement().removeChild(children.getItem(i));
			}
		}
		getElement().appendChild(html.getElement());
	}
	 
	public void addTag(String body, String color) {
		
		TaggedSpanFactory taggedSpanFactory = new TaggedSpanFactory(color);
		
		if ((lastRangeList != null) && (!lastRangeList.isEmpty())) {

			//TODO: flatten ranges to prevent multiple tagging of the same range with the same instance!
			
			RangeConverter converter = new RangeConverter();

			List<TextRange> textRanges = new ArrayList<TextRange>();
			for (Range range : lastRangeList) { 
				TextRange textRange = converter.convertToTextRange(range);
				if (!textRange.isPoint()) {
					VConsole.log("converted and adding range " + textRange );
					textRanges.add(textRange);
				}
				else {
					VConsole.log("won't tag range " + textRange + " because it is a point");
				}
			}
			
			for (TextRange textRange : textRanges) {
				NodeRange nodeRange = converter.convertToNodeRange(textRange);
				VConsole.log("adding tag to range: " + nodeRange);
				addTagToRange(taggedSpanFactory, nodeRange);
				VConsole.log("added tag to range");
			}

			if (!textRanges.isEmpty()) {
				TagInstance te = 
						new TagInstance(
								body, taggedSpanFactory.getInstanceID(), 
								taggedSpanFactory.getColor(), textRanges);
				tagInstances.put(te.getInstanceID(), te);
				sendMessage(EventAttribute.LOGMESSAGE, "TAGEVENT.toString: " + te.toString());
				sendMessage(EventAttribute.TAGINSTANCE, te.toMap());
			}
		}
		else {
			VConsole.log("no range to tag");
		}
	}

	
	public void logToServer(String logMsg) {
		sendMessage(EventAttribute.LOGMESSAGE, logMsg);
	}
	
	
	private void addTagToRange(TaggedSpanFactory taggedSpanFactory, NodeRange range) {
		
		Node startNode = range.getStartNode();
		int startOffset = range.getStartOffset();
		
		Node endNode = range.getEndNode();
		int endOffset = range.getEndOffset();
		
		DebugUtil.printNode(startNode);
		VConsole.log("startOffset: " + startOffset);
		
		DebugUtil.printNode(endNode);
		VConsole.log("endOffset: " + endOffset);

		if (startNode.equals(endNode)) {
			VConsole.log("startNode equals endNode");
			addTag(
				taggedSpanFactory, 
				startNode, startOffset, endOffset);
		}
		else {
			VConsole.log("startNode and endNode are not on the same branch");
			
			addTag(
				taggedSpanFactory, 
				startNode, startOffset, endNode, endOffset);
		}
	}
	
	private void addTag(
			TaggedSpanFactory taggedSpanFactory, 
			Node node, int originalStartOffset, int originalEndOffset) {
		
		// the whole text sequence is within one node
		
		int startOffset = Math.min(originalStartOffset, originalEndOffset);
		int endOffset = Math.max(originalStartOffset, originalEndOffset);
		String nodeText = node.getNodeValue();
		Node nodeParent = node.getParentNode();

		if (startOffset != 0) { // does the tagged sequence start at the beginning?
			// no, ok so we create a separate text node for the untagged part at the beginning
			Text t = Document.get().createTextNode(
					nodeText.substring(0, startOffset));
			nodeParent.insertBefore(t, node);
		}

		// get a list of tagged spans for every non-whitespace-containing-character-sequence 
		// and text node for the separating whitespace-sequences
		Element taggedSpan = 
				taggedSpanFactory.createTaggedSpan(
						nodeText.substring(startOffset, endOffset));
		
		// insert tagged spans and whitespace text nodes before the old node
		nodeParent.insertBefore(taggedSpan, node);

		// does the tagged sequence stretch until the end of the whole sequence? 
		if (endOffset != nodeText.length()) {
			// no, so we create a separate text node for the untagged sequence at the end
			Text t = Document.get().createTextNode(
					nodeText.substring(endOffset, nodeText.length()));
			nodeParent.insertBefore(t, node);
		}
		
		// remove the old node which is no longer needed
		nodeParent.removeChild(node);
	}

	private void addTag(
			TaggedSpanFactory taggedSpanFactory, 
			Node startNode, int startOffset, Node endNode, int endOffset) {

		AffectedNodesFinder tw = new AffectedNodesFinder(getElement(), startNode, endNode);
		
		String startNodeText = startNode.getNodeValue();
		Node startNodeParent = startNode.getParentNode();
		String endNodeText = endNode.getNodeValue();
		Node endNodeParent = endNode.getParentNode();
		
		if (endNodeText == null) { // node is a non text node like line breaks
			VConsole.log("Found no text within the following node:");
			DebugUtil.printNode(endNode);
			endNodeText = "";
		}
		
		// the range of unmarked text at the beginning of the start node's text range
		int unmarkedStartSeqBeginIdx = 0;
		int unmarkedStartSeqEndIdx = startOffset;
		
		// the marked text range of the start node
		int markedStartSeqBeginIdx = startOffset;
		int markedStartSeqEndIdx = startNodeText.length();
		
		// the range of umarked text at the end of the end node's text range
		int unmarkedEndSeqBeginIdx = endOffset;
		int unmarkedEndSeqEndIdx = endNodeText.length();
		
		// the marked text range of the end node
		int markedEndSeqBeginIdx = 0;
		int markedEndSeqEndIdx = endOffset;
		
		// if start node and end node are in reverse order within the tree 
		// we switch start/end of sequences accordingly
		if (!tw.isAfter()) {
			unmarkedStartSeqBeginIdx = startOffset;
			unmarkedStartSeqEndIdx = startNodeText.length();
			markedStartSeqBeginIdx = 0;
			markedStartSeqEndIdx = startOffset;
			
			unmarkedEndSeqBeginIdx = 0;
			unmarkedEndSeqEndIdx = endOffset;
			markedEndSeqBeginIdx = endOffset;
			markedEndSeqEndIdx = endNodeText.length();
		}
	

		// a text node for the unmarked start
		Text unmarkedStartSeq = 
			Document.get().createTextNode(
				startNodeText.substring(
						unmarkedStartSeqBeginIdx, unmarkedStartSeqEndIdx)); 

		// get a tagged span for the tagged sequence of the starting node
		Element taggedSpan = 
				taggedSpanFactory.createTaggedSpan(
						startNodeText.substring(markedStartSeqBeginIdx, markedStartSeqEndIdx));
		
		if (tw.isAfter()) {
			// insert unmarked text seqence before the old node
			startNodeParent.insertBefore(
					unmarkedStartSeq, startNode);
			// insert tagged spans before the old node
			startNodeParent.insertBefore(taggedSpan, startNode);
			// remove the old node
			startNodeParent.removeChild(startNode);
		}
		else {
			// insert tagged sequences before the old node
			startNodeParent.insertBefore(taggedSpan, startNode);
			// replace the old node with a new node for the unmarked sequence
			startNodeParent.replaceChild(
					unmarkedStartSeq, startNode);
		}

		List<Node> affectedNodes = tw.getAffectedNodes();
		DebugUtil.printNodes("affectedNodes", affectedNodes);

		// create and insert tagged sequences for all the affected text nodes
		for (int i=1; i<affectedNodes.size()-1;i++) {
			Node affectedNode = affectedNodes.get(i);
			// create the tagged span ...
			taggedSpan = 
				taggedSpanFactory.createTaggedSpan(affectedNode.getNodeValue());
			
			// ... and insert it
			affectedNode.getParentNode().insertBefore(taggedSpan, affectedNode);
			
			// remove the old node
			affectedNode.getParentNode().removeChild(affectedNode);
		}
		
		// the unmarked text sequence of the last node
		Text unmarkedEndSeq = 
			Document.get().createTextNode(
					endNodeText.substring(
							unmarkedEndSeqBeginIdx, unmarkedEndSeqEndIdx));
		
		// the tagged part of the last node
		taggedSpan = 
			taggedSpanFactory.createTaggedSpan(
						endNodeText.substring(
								markedEndSeqBeginIdx, markedEndSeqEndIdx));
		if (tw.isAfter()) {
			// insert tagged part
			endNodeParent.insertBefore(taggedSpan, endNode);
			
			// replace old node with a text node for the unmarked part
			endNodeParent.replaceChild(unmarkedEndSeq, endNode);
			
		}
		else {
			
			// insert unmarked part
			endNodeParent.insertBefore(unmarkedEndSeq, endNode);
			
			// insert tagged part
			endNodeParent.insertBefore(taggedSpan, endNode);
			// remove old node
			endNodeParent.removeChild(endNode);
		}
	}
	
	private void sendMessage(EventAttribute taggerEventAttribute, Map<String,Object> message) {
		serverConnection.updateVariable(
				clientID, taggerEventAttribute.name(), message, true);
	}
	
	private void sendMessage(EventAttribute taggerEventAttribute, String message) {
		serverConnection.updateVariable(
				clientID, taggerEventAttribute.name(), message, true);
		
	}
	
	public boolean hasSelection() {
		if ((lastRangeList != null) && !lastRangeList.isEmpty()) {
			for (Range r : lastRangeList) {
				if ((r.getEndNode()!=r.getStartNode()) | (r.getEndOffset() != r.getStartOffset())) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	public TagInstance getTagInstance(String tagInstanceID) {
		return tagInstances.get(tagInstanceID);
	}
	
	public String getTagInstanceID(String tagInstancePartID) {
		return tagInstancePartID.substring(0, tagInstancePartID.lastIndexOf("_"));
	}
	
	private TagInstance getTagInstance(ValueMap tagInstanceValueMap) {
		Map<String,Object> valueMap = new HashMap<String, Object>();
		for (String key : tagInstanceValueMap.getKeySet()) {
			valueMap.put(key, tagInstanceValueMap.getString(key));
		}

		TagInstance tagInstance = new TagInstance(valueMap);
		return tagInstance;
	}
}
