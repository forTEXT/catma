package de.catma.ui.tagger.client.ui;

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

/**
 * Client side widget which communicates with the server. Messages from the
 * server are shown as HTML and mouse clicks are sent to the server.
 */
public class VTagger extends FocusWidget 
	implements HasTagEventHandlers, Paintable, MouseUpHandler, TagEventHandler {

	public enum Attribute {
		HTML,
		TAGEVENT;
	}

	private static final String SOLIDSPACE = "&nbsp;";
	
	private static SelectionHandlerImplStandard impl = 
		 GWT.create(SelectionHandlerImplStandard.class);

	private List<Range> lastRangeList; 

	/** Set the CSS class name to allow styling. */
	public static final String CLASSNAME = "v-tagger";

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
		
		// This method call of the Paintable interface sets the component
		// style name in DOM tree
		setStyleName(CLASSNAME);
		
		// Tell GWT we are interested in receiving click events
		sinkEvents(Event.ONMOUSEUP);
//		sinkBitlessEvent(TagEvent.getName());
//		
//		// Add a handler for the click events (this is similar to FocusWidget.addClickHandler())
		addDomHandler(this, MouseUpEvent.getType());
//		addDomHandler(this, TagEvent.getType());
//		addMouseUpHandler(this);
//		addTagEventHandler(this);
	}
	
	public void onMouseUp(MouseUpEvent event) {
		lastRangeList = impl.getRangeList();
		VConsole.log("Ranges: " + lastRangeList.size());
	}
	
	public void onTagEvent(TagEvent event) {
		client.updateVariable(
			paintableId, Attribute.TAGEVENT.name(), event.toSerialization(), true);
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
		paintableId = uidl.getId();

		// Process attributes/variables from the server
		// The attribute names are the same as we used in 
		// paintContent on the server-side
		
		String html = uidl.getStringAttribute(Attribute.HTML.name());
		if (!html.isEmpty()) {
			VConsole.log("setting html");
			setHTML(new HTML(html));
		}
		String tag = uidl.getStringAttribute(Attribute.TAGEVENT.name());
		VConsole.log("tag is:" + tag);
		if (!tag.isEmpty()) {
			VConsole.log("adding tag: " + tag);
			addTag(tag);
		}
//		int clicks = uidl.getIntAttribute("clicks");
//		String message = uidl.getStringAttribute("message");
//		
//		getElement().setInnerHTML("After <b>"+clicks+"</b> mouse clicks:\n" + message);
		
		
//		
	}

 	public void setHTML(HTML html) {
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
		for (Range range : lastRangeList) {
			addTagToRange(taggedSpanFactory, range);
		}
	}
	
	public void addTagToRange(TaggedSpanFactory taggedSpanFactory, Range range) {
		
		if ((range!= null) && (!range.isEmpty())) {
			Node startNode = range.getStartNode();
			int startOffset = range.getStartOffset();
			
			Node endNode = range.getEndNode();
			int endOffset = range.getEndOffset();

			if (getElement().isOrHasChild(endNode) 
					&& getElement().isOrHasChild(startNode)) {

				if (startNode.equals(endNode)) {
					addTag(
						taggedSpanFactory, 
						startNode, startOffset, endOffset);
				}
				else {
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

	public HandlerRegistration addTagEventHandler(TagEventHandler handler) {
		return addDomHandler(handler, TagEvent.getType());
	}
	
	private void addTag(
			TaggedSpanFactory taggedSpanFactory, 
			Node startNode, int originalStartOffset, int originalEndOffset) {
		
		int startOffset = Math.min(originalStartOffset, originalEndOffset);
		int endOffset = Math.max(originalStartOffset, originalEndOffset);
		String startNodeText = startNode.getNodeValue();
		Node startNodeParent = startNode.getParentNode();
		
		if (startOffset != 0) {
			Text t = Document.get().createTextNode(
					startNodeText.substring(0, startOffset));
			startNodeParent.insertBefore(t, startNode);
		}
		
		Element taggedSpan = 
			taggedSpanFactory.createTaggedSpan(
					ensureLeadingAndTrailingSpaces(
							startNodeText.substring(startOffset, endOffset)));

		startNodeParent.insertBefore(taggedSpan, startNode);
		if (endOffset != startNodeText.length()) {
			Text t = Document.get().createTextNode(
					startNodeText.substring(endOffset, startNodeText.length()));
			startNodeParent.insertBefore(t, startNode);
		}
		startNodeParent.removeChild(startNode);
		fireEvent(new TagEvent()); //TODO: params
	}

	private void addTag(
			TaggedSpanFactory taggedSpanFactory, 
			Node startNode, int startOffset, Node endNode, int endOffset) {

		TreeWalker tw = new TreeWalker(getElement(), startNode, endNode);
		
		String startNodeText = startNode.getNodeValue();
		Node startNodeParent = startNode.getParentNode();
		String endNodeText = endNode.getNodeValue();
		Node endNodeParent = endNode.getParentNode();
		
		int unmarkedStartSeqBeginIdx = 0;
		int unmarkedStartSeqEndIdx = startOffset;
		int markedStartSeqBeginIdx = startOffset;
		int markedStartSeqEndIdx = startNodeText.length();
		
		int unmarkedEndSeqBeginIdx = endOffset;
		int unmarkedEndSeqEndIdx = endNodeText.length();
		int markedEndSeqBeginIdx = 0;
		int markedEndSeqEndIdx = endOffset;
		
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
		
		Text unmarkedStartSeq = 
			Document.get().createTextNode(
				startNodeText.substring(
						unmarkedStartSeqBeginIdx, unmarkedStartSeqEndIdx)); 
		
		Element taggedSpan = 
			taggedSpanFactory.createTaggedSpan(ensureLeadingAndTrailingSpaces(
				startNodeText.substring(
						markedStartSeqBeginIdx, markedStartSeqEndIdx)));

		startNodeParent.insertBefore(
				tw.isAfter() ? unmarkedStartSeq : taggedSpan, startNode);
		startNodeParent.replaceChild(
				tw.isAfter() ? taggedSpan : unmarkedStartSeq, startNode);
		
		List<Node> affectedNodes = tw.getAffectedNodes(); 
		for (int i=1; i<affectedNodes.size()-1;i++) {
			taggedSpan = 
				taggedSpanFactory.createTaggedSpan(affectedNodes.get(i).getNodeValue());
			affectedNodes.get(i).getParentNode().replaceChild(taggedSpan, affectedNodes.get(i));
		}
		
		Text unmarkedEndSeq = 
			Document.get().createTextNode(
					endNodeText.substring(
							unmarkedEndSeqBeginIdx, unmarkedEndSeqEndIdx));
		
		taggedSpan = 
			taggedSpanFactory.createTaggedSpan(
				ensureLeadingAndTrailingSpaces(
						endNodeText.substring(
								markedEndSeqBeginIdx, markedEndSeqEndIdx)));

		endNodeParent.insertBefore(
				tw.isAfter() ? taggedSpan : unmarkedEndSeq, endNode);
		endNodeParent.replaceChild(
				tw.isAfter() ? unmarkedEndSeq : taggedSpan, endNode);
		
		fireEvent(new TagEvent()); //TODO: params
	}

	private String ensureLeadingAndTrailingSpaces(String text) {
		if (text.length() >= 1) {
			if (text.substring(0, 1).equals(" ")) {
				text = SOLIDSPACE+text.substring(1);
			}
			
			if (text.substring(text.length()-1).equals(" ")) {
				text = text.substring(0, text.length()-1)+SOLIDSPACE;
			}
		}
		return text;
	}

}
