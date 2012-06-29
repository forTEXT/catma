package de.catma.ui.client.ui.tagger.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Text;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.vaadin.terminal.gwt.client.VConsole;

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
		MouseDownHandler, KeyUpHandler {
	
	/** Set the CSS class name to allow styling. */
	public static final String TAGGER_STYLE_CLASS = "tagger-editor";

	private static SelectionHandlerImplStandard impl = 
			 GWT.create(SelectionHandlerImplStandard.class);

	private List<Range> lastRangeList; 
	private List<TextRange> lastTextRanges;

	private HashMap<String, ClientTagInstance> tagInstances = new HashMap<String, ClientTagInstance>();
	private TaggerEditorListener taggerEditorListener;

	private String taggerID;
	
	private String lastFocusID;
	
	private int lastClientX;
	private int lastClientY;

	private List<String> lastTagInstanceIDs;
	
	public TaggerEditor(TaggerEditorListener taggerEditorListener) {
		super(Document.get().createDivElement());
		this.lastTagInstanceIDs = Collections.emptyList();
		this.taggerEditorListener = taggerEditorListener;
		
		setStylePrimaryName(TAGGER_STYLE_CLASS);
		
		// Tell GWT we are interested in consuming click events
		sinkEvents(Event.ONMOUSEUP | Event.ONMOUSEDOWN | Event.ONKEYUP);

		addMouseUpHandler(this);
		addMouseDownHandler(this);
		addKeyUpHandler(this);
		addBlurHandler(this);
		addFocusHandler(this);
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
		taggerEditorListener.tagChanged(
				TaggerEditorEventType.REMOVE, tagInstanceID, reportToServer);
	}

	public void onMouseUp(MouseUpEvent event) {
		lastRangeList = impl.getRangeList();
		VConsole.log("Ranges: " + lastRangeList.size());
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
		
		TaggedSpanFactory taggedSpanFactory = 
				new TaggedSpanFactory(tagDefinition.getColor());
		
		if (hasSelection()) {

			//TODO: flatten ranges to prevent multiple tagging of the same range with the same instance!
			
			RangeConverter converter = new RangeConverter(taggerID);

			List<TextRange> textRanges = getLastTextRanges(converter);
			
			for (TextRange textRange : textRanges) {
				NodeRange nodeRange = converter.convertToNodeRange(textRange);
				VConsole.log("adding tag to range: " + nodeRange);
				addTagInstanceForRange(taggedSpanFactory, nodeRange);
				VConsole.log("added tag to range");
			}

			if (!textRanges.isEmpty()) {
				ClientTagInstance te = 
						new ClientTagInstance(
								tagDefinition.getId(),
								taggedSpanFactory.getInstanceID(), 
								taggedSpanFactory.getColor(), textRanges);
				tagInstances.put(te.getInstanceID(), te);
				taggerEditorListener.tagChanged(TaggerEditorEventType.ADD, te);
			}
			
		}
		else {
			VConsole.log("no range to tag");
		}
		lastTextRanges = null;
	}
	
	private List<TextRange> getLastTextRanges(RangeConverter converter) {
		
		if (lastTextRanges != null) {
			return lastTextRanges;
		}
		
		ArrayList<TextRange> textRanges = new ArrayList<TextRange>();
		for (Range range : lastRangeList) { 
			if (!range.getStartNode().equals(getRootNode())
						&& ! range.getEndNode().equals(getRootNode())) {
				TextRange textRange = converter.convertToTextRange(range);
				if (!textRange.isPoint()) {
					VConsole.log("converted and adding range " + textRange );
					textRanges.add(textRange);
				}
				else {
					//TODO: consider tagging points (needs different visualization)
					VConsole.log(
						"won't tag range " + textRange + " because it is a point");
				}
			}
			else { // FIXME: in this case it seems that rootnode holds the index of
				//the last fully selected child node we could set end node to child 
				//node index with offset content lenght or something like this
				// would it work with complex nodes?
				VConsole.log(
					"won'tag range " + range + 
					" because it starts or ends with the content root");
			}
		}
		return textRanges;
	}

	private void addTagInstanceForRange(
			SpanFactory taggedSpanFactory, NodeRange range) {
		
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
			addTagInstance(
				taggedSpanFactory, 
				startNode, startOffset, endOffset);
		}
		else {
			VConsole.log("startNode and endNode are not on the same branch");
			
			addTagInstance(
				taggedSpanFactory, 
				startNode, startOffset, endNode, endOffset);
		}
	}
	
	private void addTagInstance(
			SpanFactory spanFactory, 
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
				spanFactory.createTaggedSpan(
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

	private void addTagInstance(
			SpanFactory spanFactory, 
			Node startNode, int startOffset, Node endNode, int endOffset) {

		AffectedNodesFinder tw = 
				new AffectedNodesFinder(getElement(), startNode, endNode);
		
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
			spanFactory.createTaggedSpan(
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
				spanFactory.createTaggedSpan(affectedNode.getNodeValue());
			
			VConsole.log("affected Node and its taggedSpan:");
			DebugUtil.printNode(affectedNode);
			DebugUtil.printNode(taggedSpan);
			
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
			spanFactory.createTaggedSpan(
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
	
	public boolean hasSelection() {
		VConsole.log("checking for selection");
		if ((lastTextRanges != null) && !lastTextRanges.isEmpty()) {
			VConsole.log("found lastTextRanges: " + lastTextRanges.size());
			return true;
		}
		
		if ((lastRangeList != null) && !lastRangeList.isEmpty()) {
			VConsole.log("found lastRangeList: " + lastRangeList.size());
			for (Range r : lastRangeList) {
				if ((r.getEndNode()!=r.getStartNode()) 
						|| (r.getEndOffset() != r.getStartOffset())) {
					VConsole.log("found at least on range: " + r);
					return true;
				}
			}
			VConsole.log("lastRangeList contains only a point");
		}
		
		return false;
	}
	
	public ClientTagInstance getTagInstance(String tagInstanceID) {
		return tagInstances.get(tagInstanceID);
	}
	
	public String getTagInstanceID(String tagInstancePartID) {
		return tagInstancePartID.substring(0, tagInstancePartID.lastIndexOf("_"));
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
	
			RangeConverter rangeConverter = new RangeConverter(taggerID);
	
			TaggedSpanFactory taggedSpanFactory = 
					new TaggedSpanFactory(
							tagInstance.getInstanceID(), tagInstance.getColor());
			for (TextRange textRange : tagInstance.getRanges()) {
				addTagInstanceForRange(
					taggedSpanFactory, rangeConverter.convertToNodeRange(textRange));
			}
		}
	}

	public void setTaggerID(String taggerID) {
		VConsole.log("Setting taggerID: " + taggerID);
		this.taggerID = taggerID;
	}
	
	public String getTaggerID() {
		return taggerID;
	}
	
	private Node getRootNode() {
		return Document.get().getElementById(
				ContentElementID.CONTENT.name() + taggerID);
	}

	public void highlight(TextRange textRange) {
		VConsole.log("Highlighting textrange: " + textRange);
		RangeConverter rangeConverter = new RangeConverter(taggerID);
		NodeRange nodeRange = rangeConverter.convertToNodeRange(textRange);

		HighlightedSpanFactory highlightedSpanFactory = 
				new HighlightedSpanFactory("#078E18");
		addTagInstanceForRange(highlightedSpanFactory, nodeRange);
	}
	
	
	public void onBlur(BlurEvent event) {
		VConsole.log(event.toDebugString());
		if (hasSelection()) {
			HighlightedSpanFactory highlightedSpanFactory = 
					new HighlightedSpanFactory("#3399FF");
			
			RangeConverter converter = new RangeConverter(taggerID);

			lastTextRanges = getLastTextRanges(converter);
			
			for (TextRange textRange : lastTextRanges) {
				NodeRange nodeRange = converter.convertToNodeRange(textRange);
				addTagInstanceForRange(highlightedSpanFactory, nodeRange);
			}
			
			lastFocusID = highlightedSpanFactory.getInstanceID();
		}
	}
	
	public void onFocus(FocusEvent event) {
		clearLastFocusID();
		lastTextRanges = null;
	}
	
	private void clearLastFocusID() {
		if (lastFocusID != null) {
			removeTagInstance(lastFocusID, false);
			lastFocusID = null;
		}	
	}
	
	public void onKeyUp(KeyUpEvent event) {
		lastRangeList = impl.getRangeList();
		VConsole.log("Ranges: " + lastRangeList.size());
	}

	public void onMouseDown(MouseDownEvent event) {
		lastClientX = event.getClientX();
		lastClientY = event.getClientY();
		fireTagsSelected();
	}
	
	private void fireTagsSelected() {
		Element line = findClosestLine();
		if (line != null) {
			List<Element> taggedSpans = findTargetSpan(line);
			List<String> tagInstanceIDs = new ArrayList<String>(); 
			
			for (Element span : taggedSpans) {
				tagInstanceIDs.add(0, getTagInstanceID(span.getAttribute("id")));
			}
			
			if (!tagInstanceIDs.equals(lastTagInstanceIDs)) {
				lastTagInstanceIDs = tagInstanceIDs;
				taggerEditorListener.tagsSelected(tagInstanceIDs);
			}
		}
	}

	private List<Element> findTargetSpan(Element line) {
		ArrayList<Element> result = new ArrayList<Element>();
		
		
		if (line.getFirstChildElement() != null) {

			Element curSpan = findClosestSibling(line.getFirstChildElement());
			if (curSpan != null) {
				result.add(curSpan);
			}
			while (curSpan!= null && (curSpan.getFirstChildElement()!=null)) {
				curSpan = findClosestSibling(curSpan.getFirstChildElement());
				if (curSpan != null) {
					result.add(curSpan);
				}
			}

		}
		
		return result;
	}

	private Element findClosestLine() {
		return findClosestSibling(
				Document.get().getElementById(
					ContentElementID.LINE.name() + taggerID + "0"));
	}
	
	private Element findClosestSibling(Element start) {
		Element curSibling = start;

		while((curSibling != null) && 
				!( (lastClientX > curSibling.getAbsoluteLeft()) 
						&& (lastClientX < curSibling.getAbsoluteRight())
					&& (lastClientY > curSibling.getAbsoluteTop()) 
						&& (lastClientY < curSibling.getAbsoluteBottom()))) {
			
			curSibling = curSibling.getNextSiblingElement();
		}
		
		return curSibling;
	}
}

