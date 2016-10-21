/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;

import de.catma.ui.client.ui.tagger.DebugUtil;
import de.catma.ui.client.ui.tagger.editor.TaggerEditorListener.TaggerEditorEventType;
import de.catma.ui.client.ui.tagger.impl.SelectionHandlerImplStandard;
import de.catma.ui.client.ui.tagger.impl.SelectionHandlerImplStandard.Range;
import de.catma.ui.client.ui.tagger.shared.ClientTagDefinition;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.ContentElementID;
import de.catma.ui.client.ui.tagger.shared.TextRange;

public class TaggerEditor extends FocusWidget 
	implements MouseUpHandler, BlurHandler, FocusHandler, 
		MouseDownHandler, KeyUpHandler, ClickHandler {
	private static final String LINEID_PREFIX = "LINE.";
	private static Logger logger = Logger.getLogger(TaggerEditor.class.getName());
	private static SelectionHandlerImplStandard impl = 
			 GWT.create(SelectionHandlerImplStandard.class);

	private List<Range> lastRangeList; 
	private List<NodeRange> lastTextRanges;

	private HashMap<String, ClientTagInstance> tagInstances = new HashMap<String, ClientTagInstance>();
	private TaggerEditorListener taggerEditorListener;

	private String taggerID;
	
	private String lastFocusID;
	
	private int lastClientX;
	private int lastClientY;

	private String lastTagInstancePartID = null;
	
	public TaggerEditor(TaggerEditorListener taggerEditorListener) {
		super(Document.get().createDivElement());
		
		this.taggerEditorListener = taggerEditorListener;
		
		// Tell GWT we are interested in consuming click events
		sinkEvents(Event.ONMOUSEUP | Event.ONMOUSEDOWN | Event.ONKEYUP | Event.ONCLICK);

		addMouseUpHandler(this);
		addMouseDownHandler(this);
		addKeyUpHandler(this);
		addBlurHandler(this);
		addFocusHandler(this);
		addClickHandler(this);
	}
	
	/**
	 * Can be called by external dialog and or panels to remove a tagInstance with a client side event.
	 * @param tagInstanceID the tagInstance to remove
	 */
	public void removeTagInstance(String tagInstanceID) {
		removeTagInstance(tagInstanceID, true);
	}
	
	public void removeTagInstance(String tagInstanceID, boolean reportToServer) {
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
		lastTagInstancePartID = null;
		
		taggerEditorListener.tagChanged(
				TaggerEditorEventType.REMOVE, tagInstanceID, reportToServer);
	}

	public void onMouseUp(MouseUpEvent event) {
		lastRangeList = impl.getRangeList();
		logger.info("Ranges: " + lastRangeList.size());
	}

 	public void setHTML(HTML pageHtmlContent) {
		if (getElement().hasChildNodes()) {
			NodeList<Node> children = getElement().getChildNodes();
			for (int i=0; i<children.getLength();i++) {
				getElement().removeChild(children.getItem(i));
			}
		}
		getElement().appendChild(pageHtmlContent.getElement());
		tagInstances.clear();
	}
	 
	public void createAndAddTagIntance(ClientTagDefinition tagDefinition) {
		clearLastFocusID();

		if (hasSelection()) {

			//TODO: flatten ranges to prevent multiple tagging of the same range with the same instance!
			
			List<NodeRange> nodeRanges = getLastNodeRanges();
			ClientTagInstance clientTagInstance = null;
			
			for (NodeRange nodeRange : nodeRanges) {
				logger.info("adding tag instance to range: " + nodeRange);
				ClientTagInstance currentClientTagInstance = 
					addTagInstanceForRange(IDGenerator.generate(),
						tagDefinition.getColor(), nodeRange);
				logger.info("added tag instance to range");
				if (clientTagInstance == null) {
					clientTagInstance = currentClientTagInstance;
				}
				else {
					clientTagInstance.addRanges(currentClientTagInstance.getRanges());
				}
			}

			if (clientTagInstance != null) {
				ClientTagInstance ti = 
						new ClientTagInstance(
								tagDefinition.getId(),
								clientTagInstance.getInstanceID(), 
								clientTagInstance.getColor(), 
								clientTagInstance.getRanges());
				tagInstances.put(ti.getInstanceID(), ti);
				taggerEditorListener.tagChanged(TaggerEditorEventType.ADD, ti);
			}
			
		}
		else {
			logger.info("no range to tag");
		}
		lastTextRanges = null;
	}
	
	private List<NodeRange> getLastNodeRanges() {
		
		if (lastTextRanges != null) {
			return lastTextRanges;
		}
		
		ArrayList<NodeRange> nodeRanges = new ArrayList<NodeRange>();
		for (Range range : lastRangeList) { 
			if (!range.getStartNode().equals(getRootNode())
						&& !range.getEndNode().equals(getRootNode())) {
				NodeRange nodeRange = new NodeRange(range.getStartNode(), range.getStartOffset(), 
						range.getEndNode(), range.getEndOffset());
				if (nodeRange.isPoint()) {
					//TODO: consider tagging points (needs different visualization)
					logger.info(
							"won't tag range " + nodeRange + " because it is a point");
				}
				else {
					logger.info("adding range " + nodeRange );
					nodeRanges.add(nodeRange);
				}
			}
			else {
				// one of the nodes is the root node, which is an out-of-bounds-selection
				// but we try to recover from that by looking for the next adjacent
				// text nodes
				Node startNode = range.getStartNode();
				int startOffset = range.getStartOffset();
				Node endNode = range.getEndNode();
				int endOffset = range.getEndOffset();

				if (range.getStartNode().equals(getRootNode())) {
					logger.info("startNode is root!");
					startNode = LeafFinder.getFirstTextLeaf(getRootNode().getChild(range.getStartOffset()));
					startOffset = 0;
					logger.info(
						"Setting new startNode with Offset 0: " + startNode );
				}
				
				if (range.getEndNode().equals(getRootNode())) {
					logger.info("endNode is root!");
					endNode =  LeafFinder.getFirstTextLeaf(getRootNode().getChild(range.getEndOffset()-1));
					endOffset = endNode.getNodeValue().length();
					logger.info(
						"Setting new endNode with Offset " 
								+ endOffset + ": " + endNode );
				}
				
				NodeRange nodeRange = new NodeRange(startNode, startOffset, 
						endNode, endOffset);

				if (!nodeRange.isPoint()) {
					logger.info("converted and adding range " + nodeRange);
					nodeRanges.add(nodeRange);
				}
				else {
					logger.info(
						"won't tag range " + nodeRange + " because it is a point");
				}
			}
		}

		return nodeRanges;
	}

	private TextRange validateTextRange(TextRange textRange) {
		if (textRange.getStartPos() > textRange.getEndPos()) {
			taggerEditorListener.logEvent(
				"got twisted range: " + textRange 
				+ " recovering by exchanging positions!");
			return new TextRange(textRange.getEndPos(), textRange.getStartPos());
		}
		return textRange;
	}
	
	private ClientTagInstance addTagInstanceForRange(
			String tagInstanceID, String tagColor, NodeRange range) {
		
		Node startNode = range.getStartNode();
		int startOffset = range.getStartOffset();
		
		Node endNode = range.getEndNode();
		int endOffset = range.getEndOffset();
		
		DebugUtil.printNode(startNode);
		logger.info("startOffset: " + startOffset);
		
		DebugUtil.printNode(endNode);
		logger.info("endOffset: " + endOffset);

		if (startNode.equals(endNode)) {
			logger.info("startNode equals endNode");
			return addTagInstance(
				tagInstanceID, tagColor, startOffset, endOffset, startNode);
		}
		else {
			logger.info("startNode and endNode are not on the same branch");
			
			return addTagInstance(
				tagInstanceID, tagColor,
				startNode, startOffset, endNode, endOffset);
		}
	}

	private ClientTagInstance addTagInstance(
			String tagInstanceID, String tagColor, 
			Node startNode,
			int startOffset,
			Node endNode,
			int endOffset) {
		
		Element startLineElement = getLineElementFromDisplayLayerContentNode(startNode);
		Element endLineElement = getLineElementFromDisplayLayerContentNode(endNode);
		
		if ((startLineElement != null) && (endLineElement != null)) {
			LineNodeToLineConverter startLineNodeToLineConverter = 
					new LineNodeToLineConverter(startLineElement);
			
			Line startLine = startLineNodeToLineConverter.getLine();
			ClientTagInstance clientTagInstance = addTagInstanceToLine(
				startLine, tagInstanceID, tagColor, 
				startLine.getLineOffset()+startOffset,
				startLine.getTextRange().getEndPos());
			
			LineNodeToLineConverter endLineNodeToLineConverter = 
					new LineNodeToLineConverter(endLineElement);
			
			Line endLine = endLineNodeToLineConverter.getLine();
			clientTagInstance.addRanges(
				addTagInstanceToLine(
					endLine, tagInstanceID, tagColor, 
					endLine.getTextRange().getStartPos(), 
					endLine.getLineOffset()+endOffset).getRanges());
			
			for (int lineId = startLine.getLineId()+1; lineId<endLine.getLineId(); lineId++) {
				Element lineElement = DOM.getElementById(LINEID_PREFIX+lineId);
				LineNodeToLineConverter lineNodeToLineConverter = 
						new LineNodeToLineConverter(lineElement);
				
				Line line = lineNodeToLineConverter.getLine();
				
				clientTagInstance.addRanges(
					addTagInstanceToLine(
						line, tagInstanceID, tagColor, 
						line.getTextRange().getStartPos(), 
						line.getTextRange().getEndPos()).getRanges());
			}
			
			return clientTagInstance;
		}
		
		return null;
	}
	
	private ClientTagInstance addTagInstance(
			String tagInstanceID, String tagColor, 
			int startOffset, int endOffset,
			Node node) {
		
		Element lineElement = getLineElementFromDisplayLayerContentNode(node);
		
		if (lineElement != null) {
			LineNodeToLineConverter lineNodeToLineConverter = 
					new LineNodeToLineConverter(lineElement);
			
			Line line = lineNodeToLineConverter.getLine();
			
			return addTagInstanceToLine(
				line, tagInstanceID, tagColor, 
				startOffset+line.getLineOffset(), endOffset+line.getLineOffset());
		}
		
		return null;
	}
	
	private Element getLineElementFromDisplayLayerContentNode(Node node) {
		Element displayLayerContent = node.getParentElement().getParentElement();
		if (displayLayerContent.hasClassName("tagger-display-layer")) {
			return displayLayerContent.getParentElement().getParentElement();
		}
		return null;
	}
	
	private ClientTagInstance addTagInstanceToLine(Line line, String tagInstanceID, String tagColor, 
			int startOffset, int endOffset) {

		TextRange textRange = new TextRange(startOffset, endOffset);
		List<TextRange> textRanges = new ArrayList<>();
		textRanges.add(textRange);

		ClientTagInstance clientTagInstance = new ClientTagInstance(null, tagInstanceID, tagColor, textRanges);
		line.addTagInstance(clientTagInstance);
		
		line.updateLineElement();	
		
		return clientTagInstance;
	}

	public boolean hasSelection() {
		logger.info("checking for selection");
		if ((lastTextRanges != null) && !lastTextRanges.isEmpty()) {
			logger.info("found lastTextRanges: " + lastTextRanges.size());
			return true;
		}
		
		if ((lastRangeList != null) && !lastRangeList.isEmpty()) {
			logger.info("found lastRangeList: " + lastRangeList.size());
			for (Range r : lastRangeList) {
				if ((r.getEndNode()!=r.getStartNode()) 
						|| (r.getEndOffset() != r.getStartOffset())) {
					logger.info("found at least one range: " + r);
					return true;
				}
			}
			logger.info("lastRangeList contains only a point");
		}
		
		return false;
	}
	
	public ClientTagInstance getTagInstance(String tagInstanceID) {
		return tagInstances.get(tagInstanceID);
	}
	
	public String getTagInstanceID(String tagInstancePartID) {
		return ClientTagInstance.getTagInstanceIDFromPartId(tagInstancePartID);
	}

	public void clearTagInstances() {
		ArrayList<String> keyCopy = new ArrayList<String>();
		keyCopy.addAll(tagInstances.keySet());
		for (String tagInstanceID : keyCopy) {
			removeTagInstance(tagInstanceID, false);
		}		
	}

	public void addTagInstance(ClientTagInstance tagInstance) {
		
		if (!tagInstances.containsKey(tagInstance.getInstanceID())) {
			
			tagInstances.put(tagInstance.getInstanceID(), tagInstance);

			logger.info("TAGINSTANCES size: " + tagInstances.size());
		}
		
	}

	public void setTaggerID(String taggerID) {
		logger.info("Setting taggerID: " + taggerID);
		this.taggerID = taggerID;
	}
	
	public String getTaggerID() {
		return taggerID;
	}
	
	private Node getRootNode() {
		return Document.get().getElementById(
				ContentElementID.CONTENT.name() + taggerID);
	}

	//TODO: not needed for now, but could be usefull for a quicksearch facility within the tagger
	public void highlight(TextRange textRange) {
		logger.info("Highlighting textrange: " + textRange);
//		for (int lineId : lineIds) {
//			Element lineElement = Document.get().getElementById(LINEID_PREFIX+lineId);
//			LineNodeToLineConverter lineNodeToLineConverter = new LineNodeToLineConverter(lineElement);
//			Line line = lineNodeToLineConverter.getLine();
//			line.addHighlightedTextRange(textRange);
//			line.updateLineElement();
//		}
	}
	
	//TODO: reimplement
	public void onBlur(BlurEvent event) {
		if (hasSelection()) {
			List<NodeRange> lastNodeRanges = getLastNodeRanges();
			for (NodeRange nodeRange : lastNodeRanges) {
				Node startNode = nodeRange.getStartNode();
				int startOffset = nodeRange.getStartOffset();
				
				Node endNode = nodeRange.getEndNode();
				int endOffset = nodeRange.getEndOffset();		
				
				if (startNode.equals(endNode)) {
					Element lineElement = getLineElementFromDisplayLayerContentNode(startNode);
					LineNodeToLineConverter lineNodeToLineConverter = new LineNodeToLineConverter(lineElement);
					Line line = lineNodeToLineConverter.getLine();
					TextRange textRange =
						new TextRange(
							line.getLineOffset()+startOffset, line.getLineOffset()+endOffset);
					line.addHighlightedTextRange(textRange);
					line.updateLineElement();
				}
				else {
					Element startLineElement = getLineElementFromDisplayLayerContentNode(startNode);
					Element endLineElement = getLineElementFromDisplayLayerContentNode(endNode);
					LineNodeToLineConverter startLineConverter = new LineNodeToLineConverter(startLineElement);
					Line startLine = startLineConverter.getLine();
					LineNodeToLineConverter endLineConverter = new LineNodeToLineConverter(endLineElement);
					Line endLine = endLineConverter.getLine();

					TextRange startRange = 
						new TextRange(startLine.getLineOffset()+startOffset, startLine.getTextRange().getEndPos());
					startLine.addHighlightedTextRange(startRange);
					startLine.updateLineElement();
					
					TextRange endRange = 
						new TextRange(endLine.getLineOffset(), endLine.getLineOffset() + endOffset);
					endLine.addHighlightedTextRange(endRange);
					endLine.updateLineElement();
					
					for (int lineId = startLine.getLineId()+1; lineId<endLine.getLineId(); lineId++) {
						Element lineElement = DOM.getElementById(LINEID_PREFIX+lineId);
						LineNodeToLineConverter lineNodeToLineConverter = new LineNodeToLineConverter(lineElement);
						Line line = lineNodeToLineConverter.getLine();
						line.addHighlightedTextRange(line.getTextRange());
						line.updateLineElement();
					}
				}
			}
		}
//		logger.info(event.toDebugString());
//		if (hasSelection()) {
//			HighlightedSpanFactory highlightedSpanFactory = 
//					new HighlightedSpanFactory("#3399FF");
//			
//			RangeConverter converter = new RangeConverter(taggerID);
//
//			lastTextRanges = getLastTextRanges(converter);
//			
//			for (TextRange textRange : lastTextRanges) {
//				NodeRange nodeRange = converter.convertToNodeRange(textRange);
//				addTagInstanceForRange(highlightedSpanFactory, nodeRange);
//			}
//			
//			lastFocusID = highlightedSpanFactory.getInstanceID();
//		}
	}
	//TODO: reimplement
	public void onFocus(FocusEvent event) {
		clearLastFocusID();
		lastTextRanges = null;
	}
	//TODO: reimplement
	private void clearLastFocusID() {
		if (lastFocusID != null) {
			removeTagInstance(lastFocusID, false);
			lastFocusID = null;
		}	
	}
	
	public void onKeyUp(KeyUpEvent event) {
		lastRangeList = impl.getRangeList();
		logger.info("Ranges: " + lastRangeList.size());
	}

	public void onMouseDown(MouseDownEvent event) {
		lastClientX = event.getClientX();
		lastClientY = event.getClientY();
		logger.info("mouse down at: " + lastClientX + "," + lastClientY);
	}
	

	private void fireTagsSelected(Element targetElement) {

		if (targetElement.getParentElement().hasClassName("annotation-layer")) {
			String tagInstancePartId = targetElement.getAttribute("id");
			if (tagInstancePartId.isEmpty() && (lastTagInstancePartID == null)) {
				return; // no annotation present 
			}
			String tagInstanceId = getTagInstanceID(tagInstancePartId);
			
			setTagInstanceSelected(tagInstanceId);
			
			String lineID = getLineID(targetElement);
			
			if (!tagInstanceId.isEmpty()) {
				if ((lastTagInstancePartID == null) 
						|| ( ! lastTagInstancePartID.equals(tagInstancePartId))) {
					if (isKnownTagInstanceID(tagInstanceId)) {
						this.lastTagInstancePartID = tagInstancePartId;
						logger.info("fireTagsSelected: notifying listeners");
						taggerEditorListener.tagSelected(tagInstancePartId, lineID);
					}
				}
			}
			else {
				lastTagInstancePartID = null;
			}
		}
	}
	
	private Integer getRedFromCssRgb(String cssColor) {
		RegExp c = RegExp.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
	    MatchResult m = c.exec(cssColor);

	    if (m != null) { 
	    	return Integer.valueOf(m.getGroup(1)); 
	    }
	
		return 0;
	}
	
	private String getLineID(Element targetElement) {
		
		Element annotationLayer = targetElement.getParentElement();
		Element lineElement = annotationLayer.getParentElement().getParentElement();
		
		return lineElement.getAttribute("id");
	}

	private boolean isKnownTagInstanceID(String tagInstanceID) {
		logger.info("fireTagsSelected: testing tagInstanceID " + tagInstanceID);
		if (tagInstances.containsKey(tagInstanceID)) {
			logger.info("fireTagsSelected: known tagInstanceID found " + tagInstanceID);
			return true;
		}
		
		return false;
	}

	@Override
	public void onClick(ClickEvent event) {
		EventTarget eventTarget = event.getNativeEvent().getEventTarget();
		if (Element.is(eventTarget)) {
			Element targetElement = Element.as(eventTarget);
			fireTagsSelected(targetElement);
		}
	}

	public void setTagInstanceSelected(String tagInstanceId) {
		Element rootElement = Element.as(getRootNode());
		
		NodeList<Element> tagPartElements = rootElement.getElementsByTagName("td");

		for (int index = 0; index<=tagPartElements.getLength(); index++) {
			Element tagPartElement = tagPartElements.getItem(index);
			if (tagPartElement != null) {
				if (!tagInstanceId.isEmpty()
					&& tagPartElement.hasAttribute("id") 
					&& tagPartElement.getAttribute("id").startsWith(tagInstanceId)) {
					
					String cssRgbColor = tagPartElement.getStyle().getColor();
					int red = getRedFromCssRgb(cssRgbColor);
					if (red > 170) {
						tagPartElement.addClassName("selected-tag-instance-black");
					}
					else {
						tagPartElement.addClassName("selected-tag-instance-red");
					}
					tagPartElement.removeClassName("unselected-tag-instance");
				}
				else if (tagPartElement.hasClassName("selected-tag-instance-black")) {
					tagPartElement.removeClassName("selected-tag-instance-black");
					tagPartElement.addClassName("unselected-tag-instance");
				}
				else if (tagPartElement.hasClassName("selected-tag-instance-red")) {
					tagPartElement.removeClassName("selected-tag-instance-red");
					tagPartElement.addClassName("unselected-tag-instance");
				}

			}
		}
	}
}

