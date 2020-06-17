package de.catma.ui.module.annotate.pager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.vaadin.icons.VaadinIcons;

import de.catma.document.comment.Comment;
import de.catma.ui.client.ui.tagger.shared.AnnotationLayerBuilder;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import nu.xom.Attribute;
import nu.xom.Element;

public class Line {

	private static final String PLUS_CODE = "\uE801";
	
	private int lineId;
	private List<LineContent> lineContents;
	private TextRange textRange;
	private int lineLength = 0;

	private Multimap<String, TextRange> textRangesByRelativeTagInstanceID;
	private Map<String,ClientTagInstance> relativeTagInstanceByID;
	private Set<TextRange> highlightedTextRanges;
	private boolean rightToLeftWriting;
	private Set<Comment> comments;

	public Line(boolean rightToLeftWriting) {
		this.rightToLeftWriting = rightToLeftWriting;
		lineContents = new ArrayList<>();
		textRangesByRelativeTagInstanceID = ArrayListMultimap.create();
		relativeTagInstanceByID = new HashMap<>();
		highlightedTextRanges = new HashSet<>();
		comments = new HashSet<>();
	}
	
	public String getPresentationContent() {
		StringBuilder builder = new StringBuilder();
		for (LineContent lc : lineContents) {
			builder.append(lc.getPresentationContent());
		}
		
		return builder.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (LineContent lc : lineContents) {
			builder.append(lc);
		}
		return "#"+lineId+textRange+builder.toString(); //$NON-NLS-1$
	}

	public void addCharacterContent(String content) {
		lineContents.add(new CharacterContent(content, lineLength));
		lineLength+=content.length();
	}
	
	public void addWhitespaceContent(String content) {
		lineContents.add(new WhitespaceContent(content, lineLength));
		lineLength+=content.length();
	}

	public void addLineSeparatorContent(String content) {
		lineContents.add(new LineSeparatorContent(content, lineLength));
		lineLength+=content.length();
	}

	public void setLineId(int lineId) {
		this.lineId = lineId;
	}
	
	public int getLineId() {
		return lineId;
	}
	
	public void setTextRange(TextRange relativeTextRange) {
		this.textRange = relativeTextRange;
		for (LineContent lineContent : lineContents) {
			lineContent.setLineOffset(relativeTextRange.getStartPos());
		}
	}

	public boolean containsTextRange(TextRange tr) {
		return tr.hasOverlappingRange(this.textRange);
	}

	public void addRelativeTagInstanceTextRange(
			TextRange tr, ClientTagInstance relativeTagInstance) {
		textRangesByRelativeTagInstanceID.put(
				relativeTagInstance.getInstanceID(), tr);
		relativeTagInstanceByID.put(
				relativeTagInstance.getInstanceID(), relativeTagInstance);
	}
	
	public Element toHTML() {
		
		List<TextRange> rangeParts = new ArrayList<>();
		
		rangeParts.add(new TextRange(this.textRange));
		
		Set<TextRange> tagInstanceAndHighlightedTextRanges = new HashSet<>(textRangesByRelativeTagInstanceID.values());
		tagInstanceAndHighlightedTextRanges.addAll(highlightedTextRanges);
		
		for (TextRange currentTextRange : tagInstanceAndHighlightedTextRanges) {
			
			for (TextRange rangePart : new TreeSet<TextRange>(rangeParts)) {
				if (rangePart.hasOverlappingRange(currentTextRange)) {
					rangeParts.remove(rangePart);
					rangeParts.add(rangePart.getOverlappingRange(currentTextRange));
					for (TextRange disjointRange : 
							rangePart.getDisjointRanges(currentTextRange)) {
						
						if (rangePart.hasOverlappingRange(disjointRange)) {
							rangeParts.add(disjointRange);
						}
						else {
							currentTextRange = disjointRange;
						}
					}
				}
			}
			
		}
		
		Collections.sort(rangeParts);
//		
//		for (TextRange rangePart : rangeParts) {
//			System.out.println(rangePart);
//		}
//

		Element table = new Element("table"); //$NON-NLS-1$
		table.addAttribute(new Attribute("class", "taggerline-table")); //$NON-NLS-1$ //$NON-NLS-2$
		table.addAttribute(new Attribute("id", "LINE." + String.valueOf(lineId))); //$NON-NLS-1$ //$NON-NLS-2$
		
		Element tbody = new Element("tbody"); //$NON-NLS-1$
		table.appendChild(tbody);
		
		// display layer
		
		Element contentDisplayLayer = new Element("tr"); //$NON-NLS-1$
		contentDisplayLayer.addAttribute(new Attribute("class", "tagger-display-layer")); //$NON-NLS-1$ //$NON-NLS-2$
		contentDisplayLayer.addAttribute(new Attribute("id", textRange.getStartPos()+"."+textRange.getEndPos())); //$NON-NLS-1$ //$NON-NLS-2$
		
		tbody.appendChild(contentDisplayLayer);
		Element visibleContentLayerContent = new Element("td"); //$NON-NLS-1$
		contentDisplayLayer.appendChild(visibleContentLayerContent);
		visibleContentLayerContent.addAttribute(
				new Attribute("colspan", String.valueOf(rangeParts.size()))); //$NON-NLS-1$
		visibleContentLayerContent.appendChild(getPresentationContent());
		
		// highlight layer
		if (!highlightedTextRanges.isEmpty()) {
			Element highlightLayer = new Element("tr"); //$NON-NLS-1$
			highlightLayer.addAttribute(new Attribute("class", "highlight-layer")); //$NON-NLS-1$ //$NON-NLS-2$
			highlightLayer.addAttribute(new Attribute("unselectable", "on")); //$NON-NLS-1$ //$NON-NLS-2$
			tbody.appendChild(highlightLayer);
			
			for (TextRange rangePart : rangeParts) {
				Element highlightLayerContent = new Element("td"); //$NON-NLS-1$
				highlightLayer.appendChild(highlightLayerContent);
				highlightLayerContent.appendChild(TextRange.NBSP);
				highlightLayerContent.addAttribute(
					new Attribute(
						"id",  //$NON-NLS-1$
						"h."+rangePart.getStartPos()+"."+rangePart.getEndPos())); //$NON-NLS-1$ //$NON-NLS-2$

				if (rangePartIsHighlighted(rangePart)) {
					highlightLayerContent.addAttribute(
							new Attribute("class", "highlighted-content")); //$NON-NLS-1$ //$NON-NLS-2$
				}
				else {
					highlightLayerContent.addAttribute(
							new Attribute("class", "empty-highlight-layer")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		
		// comment layer
		Element commentLayer = new Element("tr"); //$NON-NLS-1$
		commentLayer.addAttribute(new Attribute("class", "comment-layer")); //$NON-NLS-1$ //$NON-NLS-2$
		commentLayer.addAttribute(new Attribute("unselectable", "on")); //$NON-NLS-1$ //$NON-NLS-2$
		tbody.appendChild(commentLayer);
		
		Element commentContent = new Element("td"); //$NON-NLS-1$
		commentLayer.appendChild(commentContent);
		commentContent.addAttribute(
				new Attribute("class", "empty-comment-layer")); //$NON-NLS-1$ //$NON-NLS-2$
		
		
//		Element commentAnchor = new Element("td"); //$NON-NLS-1$
//		commentLayer.appendChild(commentAnchor);
		commentContent.addAttribute(
				new Attribute("class", "comment-anchor")); //$NON-NLS-1$ //$NON-NLS-2$

		Element commentContainer = new Element("div");
		commentContent.appendChild(commentContainer);
		commentContainer.addAttribute(
				new Attribute("class", "comment-container")); //$NON-NLS-1$ //$NON-NLS-2$
		
		// TODO: client side:
//		for (Comment comment : comments) {
//			Element commentDiv = new Element("div"); //$NON-NLS-1$
//			commentDiv.addAttribute(
//					new Attribute("class", "comment")); //$NON-NLS-1$ //$NON-NLS-2$
//			commentContainer.appendChild(commentDiv);
//			commentDiv.appendChild(comment.getBody());
//		}
		
		
		// annotation layers
		AnnotationLayerBuilder annotationLayerBuilder = new AnnotationLayerBuilder(relativeTagInstanceByID.values(), rangeParts);
		Table<Integer, TextRange, ClientTagInstance> layerTable = annotationLayerBuilder.getLayerTable();
		int rowCount = layerTable.rowKeySet().size();
		for (int rowIdx = 0; rowIdx<rowCount; rowIdx++) {
			Element annotationLayer = new Element("tr"); //$NON-NLS-1$
			
			annotationLayer.addAttribute(new Attribute("class", "annotation-layer")); //$NON-NLS-1$ //$NON-NLS-2$
			annotationLayer.addAttribute(new Attribute("unselectable", "on")); //$NON-NLS-1$ //$NON-NLS-2$
			tbody.appendChild(annotationLayer);

			for (TextRange rangePart : rangeParts) {
				
				Element annotationLayerContent = new Element("td"); //$NON-NLS-1$
				annotationLayer.appendChild(annotationLayerContent);
				annotationLayerContent.appendChild(TextRange.NBSP);
				
				ClientTagInstance relativeTagInstance = 
						layerTable.get(rowIdx, rangePart);
				
				if (relativeTagInstance != null) {
					annotationLayerContent.addAttribute(new Attribute("class", "unselected-tag-instance")); //$NON-NLS-1$ //$NON-NLS-2$
					
					annotationLayerContent.addAttribute(
						new Attribute(
							"id",  //$NON-NLS-1$
							relativeTagInstance.getInstanceID() 
								+ "." //$NON-NLS-1$
								+ rangePart.getStartPos()
								+ "." //$NON-NLS-1$
								+ rangePart.getEndPos()));
					annotationLayerContent.addAttribute(
						new Attribute(
								"style",  //$NON-NLS-1$
								"background:#"+relativeTagInstance.getColor() //$NON-NLS-1$
								+";color:#"+relativeTagInstance.getColor())); //$NON-NLS-1$
				}
				else {
					annotationLayerContent.addAttribute(
							new Attribute("class", "empty-annotation-layer")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		
		// segmentation layer
		Element segmentationLayer = new Element("tr"); //$NON-NLS-1$

		segmentationLayer.addAttribute(new Attribute("class", "segmentation-layer")); //$NON-NLS-1$ //$NON-NLS-2$
		segmentationLayer.addAttribute(new Attribute("unselectable", "on")); //$NON-NLS-1$ //$NON-NLS-2$
		tbody.appendChild(segmentationLayer);
		String lastPresentationContent = null;
		for (TextRange rangePart : rangeParts) {
			
			Element segmentationLayerContent = new Element("td"); //$NON-NLS-1$
			segmentationLayer.appendChild(segmentationLayerContent);
			String presentationContent = getPresentationContent(rangePart);
			
			if (rightToLeftWriting 
				&& (lastPresentationContent != null) 
				&& !lastPresentationContent.endsWith(TextRange.NBSP)) {
				
				segmentationLayerContent.appendChild(presentationContent+TextRange.ZWJ);
			}
			else {
				segmentationLayerContent.appendChild(presentationContent);
			}
			lastPresentationContent = presentationContent;
		}
		
		return table;
	}
	
	private boolean rangePartIsHighlighted(TextRange rangePart) {
		for (TextRange highlightedTextRange : highlightedTextRanges) {
			if (rangePart.isCoveredBy(highlightedTextRange)) {
				return true;
			}
		}
		return false;
	}

	private String getPresentationContent(TextRange rangePart) {
		StringBuilder builder = new StringBuilder();
		for (LineContent lineContent : lineContents) {
			if (lineContent.hasOverlappingRange(rangePart)) {
				builder.append(lineContent.getPresentationContent(rangePart));
			}
		}
		return builder.toString();
	}

	public void removeRelativeTagInstance(String tagInstanceID) {
		ClientTagInstance relativeTagInstance = relativeTagInstanceByID.remove(tagInstanceID);
		if (relativeTagInstance != null) {
			textRangesByRelativeTagInstanceID.removeAll(relativeTagInstance.getInstanceID());
		}
	}

	public void clearRelativeTagInstanes() {
		relativeTagInstanceByID.clear();
		textRangesByRelativeTagInstanceID.clear();
	}

	public List<String> getTagInstanceIDs(String instancePartID) {
		TextRange range = ClientTagInstance.getTextRangeFromPartId(instancePartID);
		
		ArrayList<String> result = new ArrayList<String>();
		
		for (Map.Entry<String, Collection<TextRange>> entry : textRangesByRelativeTagInstanceID.asMap().entrySet()) {
			
			if (range.isCoveredBy(entry.getValue())) {
				result.add(entry.getKey());
			}
		}
		
		return result;
	}

	public String getTagInstanceID(String instancePartID) {
		return ClientTagInstance.getTagInstanceIDFromPartId(instancePartID);
	}

	public TextRange getOverlappingRange(TextRange textRange) {
		return this.textRange.getOverlappingRange(textRange);
	}

	public void addHighlight(TextRange highlightRange) {
		highlightedTextRanges.add(highlightRange);
	}

	public boolean hasHighlights() {
		return !highlightedTextRanges.isEmpty();
	}
	
	public void removeHighlights() {
		highlightedTextRanges.clear();
	}

}

