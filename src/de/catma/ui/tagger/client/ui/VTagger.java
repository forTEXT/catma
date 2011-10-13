package de.catma.ui.tagger.client.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Text;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.vaadin.terminal.gwt.client.ApplicationConnection;
import com.vaadin.terminal.gwt.client.Paintable;
import com.vaadin.terminal.gwt.client.UIDL;
import com.vaadin.terminal.gwt.client.VConsole;

import de.catma.ui.tagger.client.ui.impl.SelectionHandlerImplStandard;
import de.catma.ui.tagger.client.ui.impl.SelectionHandlerImplStandard.Range;
import de.catma.ui.tagger.client.ui.shared.TaggedNode;
import de.catma.ui.tagger.client.ui.shared.TaggerEventAttribute;

/**
 * Client side widget which communicates with the server. Messages from the
 * server are shown as HTML and mouse clicks are sent to the server.
 */
public class VTagger extends FocusWidget 
	implements HasTagEventHandlers, Paintable, MouseUpHandler, TagEventHandler {

	private static enum Direction {
		RIGHT,
		LEFT;
	}

	/** Set the CSS class name to allow styling. */
	public static final String TAGGER_STYLE_CLASS = "v-tagger";

	private static SelectionHandlerImplStandard impl = 
			 GWT.create(SelectionHandlerImplStandard.class);

	private List<Range> lastRangeList; 

	/** The client side widget identifier */
	protected String paintableId;

	/** Reference to the server connection object. */
	protected ApplicationConnection client;

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

		setStyleName(TAGGER_STYLE_CLASS);
		
		// Tell GWT we are interested in consuming click events
		sinkEvents(Event.ONMOUSEUP);

		addMouseUpHandler(this);
		addTagEventHandler(this);
	}
	
	public void onMouseUp(MouseUpEvent event) {
		lastRangeList = impl.getRangeList();
		VConsole.log("Ranges: " + lastRangeList.size());
	}
	
	public void onTagEvent(TagEvent event) {
		client.updateVariable(
			paintableId, TaggerEventAttribute.TAGEVENT.name(), event.toString(), true);
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
		this.client = client;

		// Save the client side identifier (paintable id) for the widget
		this.paintableId = uidl.getId();

		if (uidl.hasAttribute(TaggerEventAttribute.HTML.name())) {
			VConsole.log("setting html");
			setHTML(new HTML(uidl.getStringAttribute(TaggerEventAttribute.HTML.name())));
		}
		if (uidl.hasAttribute(TaggerEventAttribute.TAGEVENT.name())) {
			String tag = uidl.getStringAttribute(TaggerEventAttribute.TAGEVENT.name());
			VConsole.log("adding tag: " + tag);
			addTag(tag);
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
	 
	public void addTag(String tag) {
		
		TaggedSpanFactory taggedSpanFactory = new TaggedSpanFactory(tag);
		if (lastRangeList != null) {
			for (Range range : lastRangeList) { 
				//FIXME: wenn ranges im selben knoten sind, wir nur die erste markiert, 
				//weil nach Erstellen des ersten Tags, der Baum verändert wurde.
				VConsole.log("adding tag to range: " + range);
				addTagToRange(taggedSpanFactory, range);
				VConsole.log("added tag to range");
			}
		}
		else {
			VConsole.log("no range to tag");
		}
	}
	
	public void addTagToRange(TaggedSpanFactory taggedSpanFactory, Range range) {
		
		if ((range!= null) && (!range.isEmpty())) {
			
			Node startNode = range.getStartNode();
			int startOffset = range.getStartOffset();
			
			Node endNode = range.getEndNode();
			int endOffset = range.getEndOffset();
			
			DebugUtil.printNode(startNode);
			VConsole.log("startOffset: " + startOffset);
			
			DebugUtil.printNode(endNode);
			VConsole.log("endOffset: " + endOffset);

			
			if (getElement().isOrHasChild(endNode) 
					&& getElement().isOrHasChild(startNode)) {

				if (Element.is(startNode)) {
					startNode = findClosestTextNode(startNode.getChild(startOffset),Direction.RIGHT);
					VConsole.log("Found closes text node for startNode: ");
					DebugUtil.printNode(startNode);
					startOffset = 0;
				}
	
				if (Element.is(endNode)) {
					endNode = findClosestTextNode(endNode.getChild(endOffset), Direction.LEFT);
					VConsole.log("Found closes text node for endNode: ");
					DebugUtil.printNode(endNode);
					endOffset = endNode.getNodeValue().length();
				}

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
			else {
				VConsole.log("at least one node is out of the tagger's bounds");
			}
		}
		else {
			VConsole.log("range is empty or out of the tagger's bounds");
		}
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
	
	public HandlerRegistration addTagEventHandler(TagEventHandler handler) {
		return addHandler(handler, TagEvent.getType());
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

		TagEvent te = new TagEvent(
					taggedSpanFactory.getTag(), 
						new TaggedNode(
								Element.as(nodeParent).getId(), 
								startOffset, endOffset, taggedSpan.getId()));
		VConsole.log("firing event " +  te.toString());
		fireEvent(te); 
	}

	private void addTag(
			TaggedSpanFactory taggedSpanFactory, 
			Node startNode, int startOffset, Node endNode, int endOffset) {

		AffectedNodesFinder tw = new AffectedNodesFinder(getElement(), startNode, endNode);
		List<TaggedNode> taggedNodes = new ArrayList<TaggedNode>();
		
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
		
		

		if (!hasTag(startNode, taggedSpanFactory.getTag())) {
			
			// a text node for the unmarked start
			Text unmarkedStartSeq = 
				Document.get().createTextNode(
					startNodeText.substring(
							unmarkedStartSeqBeginIdx, unmarkedStartSeqEndIdx)); 
	
			// get a tagged span for the tagged sequence of the starting node
			Element taggedSpan = 
					taggedSpanFactory.createTaggedSpan(
							startNodeText.substring(markedStartSeqBeginIdx, markedStartSeqEndIdx));
			taggedNodes.add(
					new TaggedNode(
							startNode.getParentElement().getId(), 
							markedStartSeqBeginIdx, markedStartSeqEndIdx,
							taggedSpan.getId()));
			
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
		}
		List<Node> affectedNodes = tw.getAffectedNodes();
		DebugUtil.printNodes("affectedNodes", affectedNodes);

		// create and insert tagged sequences for all the affected text nodes
		for (int i=1; i<affectedNodes.size()-1;i++) {
			// create the tagged span ...
			Element taggedSpan = 
				taggedSpanFactory.createTaggedSpan(affectedNodes.get(i).getNodeValue());
			
			taggedNodes.add(
					new TaggedNode(
							affectedNodes.get(i).getParentElement().getId(), 
							affectedNodes.get(i).getNodeValue().length(),
							taggedSpan.getId()));
			
			// ... and insert it
			affectedNodes.get(i).getParentNode().insertBefore(taggedSpan, affectedNodes.get(i));
			
			// remove the old node
			affectedNodes.get(i).getParentNode().removeChild(affectedNodes.get(i));
		}
		
		// the unmarked text sequence of the last node
		Text unmarkedEndSeq = 
			Document.get().createTextNode(
					endNodeText.substring(
							unmarkedEndSeqBeginIdx, unmarkedEndSeqEndIdx));
		
		// the tagged part of the last node
		Element taggedSpan = 
			taggedSpanFactory.createTaggedSpan(
						endNodeText.substring(
								markedEndSeqBeginIdx, markedEndSeqEndIdx));
		taggedNodes.add(
				new TaggedNode(
						endNode.getParentElement().getId(), 
						markedEndSeqBeginIdx, markedEndSeqEndIdx,
						taggedSpan.getId()));

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
		
		
		TagEvent te = new TagEvent(taggedSpanFactory.getTag(), taggedNodes);
		VConsole.log("firing event " +  te.toDebugString());
		fireEvent(te); 
	}
	
	private boolean hasTag(Node node, String tag) {
	// TODO: shall we allow multiple tagging with the same tag (maybe different non-contiguous tags or different properties!!!)
		
//		Element current = node.getParentElement();
//		do {
//			if (current.getClassName().equals(tag)) {
//				return true;
//			}
//			current = current.getParentElement();
//		}
//		while(current != null);
		
		return false; 
	}
}
