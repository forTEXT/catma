package de.catma.ui.tagger.pager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import de.catma.ui.client.ui.tagger.shared.AnnotationLayerBuilder;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import nu.xom.Attribute;
import nu.xom.Element;

public class Line {

	private int lineId;
	private List<LineContent> lineContents;
	private TextRange textRange;
	private int lineLength = 0;

	private Multimap<String, TextRange> textRangesByRelativeTagInstanceID;
	private Map<String,ClientTagInstance> relativeTagInstanceByID;
	

	public Line() {
		lineContents = new ArrayList<>();
		textRangesByRelativeTagInstanceID = ArrayListMultimap.create();
		relativeTagInstanceByID = new HashMap<>();
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
		return "#"+lineId+textRange+builder.toString();
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
	
	public void setTextRange(TextRange textRange) {
		this.textRange = textRange;
		for (LineContent lineContent : lineContents) {
			lineContent.setLineOffset(textRange.getStartPos());
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
		
		for (TextRange currentTextRange : textRangesByRelativeTagInstanceID.values()) {
			
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
		
		for (TextRange rangePart : rangeParts) {
			System.out.println(rangePart);
		}


		Element table = new Element("table");
		table.addAttribute(new Attribute("class", "taggerline-table"));
		table.addAttribute(new Attribute("id", "LINE." + String.valueOf(lineId)));
		
		Element tbody = new Element("tbody");
		table.appendChild(tbody);
		
		Element contentDisplayLayer = new Element("tr");
		contentDisplayLayer.addAttribute(new Attribute("class", "tagger-display-layer"));
		contentDisplayLayer.addAttribute(new Attribute("id", textRange.getStartPos()+"."+textRange.getEndPos()));
		
		tbody.appendChild(contentDisplayLayer);
		Element visibleContentLayerContent = new Element("td");
		contentDisplayLayer.appendChild(visibleContentLayerContent);
		visibleContentLayerContent.addAttribute(
				new Attribute("colspan", String.valueOf(rangeParts.size())));
		visibleContentLayerContent.appendChild(getPresentationContent());
		
		AnnotationLayerBuilder annotationLayerBuilder = new AnnotationLayerBuilder(relativeTagInstanceByID.values(), rangeParts);
		Table<Integer, TextRange, ClientTagInstance> layerTable = annotationLayerBuilder.getLayerTable();
		int rowCount = layerTable.rowKeySet().size();
		for (int rowIdx = 0; rowIdx<rowCount; rowIdx++) {
			Element annotationLayer = new Element("tr");
			
			annotationLayer.addAttribute(new Attribute("class", "annotation-layer"));
			annotationLayer.addAttribute(new Attribute("unselectable", "on"));
			tbody.appendChild(annotationLayer);

			for (TextRange rangePart : rangeParts) {
				
				Element annotationLayerContent = new Element("td");
				annotationLayer.appendChild(annotationLayerContent);
				annotationLayerContent.appendChild(TextRange.NBSP);
				
				ClientTagInstance relativeTagInstance = 
						layerTable.get(rowIdx, rangePart);
				
				if (relativeTagInstance != null) {
					annotationLayerContent.addAttribute(new Attribute("class", "unselected-tag-instance"));
					
					annotationLayerContent.addAttribute(
						new Attribute(
							"id", 
							relativeTagInstance.getInstanceID() 
								+ "."
								+ rangePart.getStartPos()
								+ "."
								+ rangePart.getEndPos()));
					annotationLayerContent.addAttribute(
						new Attribute(
								"style", 
								"background:#"+relativeTagInstance.getColor()
								+";color:#"+relativeTagInstance.getColor()));
				}
				else {
					annotationLayerContent.addAttribute(
							new Attribute("class", "empty-annotation-layer"));
				}
			}
		}
		System.out.println(tbody.toXML());
		
		Element segmentationLayer = new Element("tr");

		segmentationLayer.addAttribute(new Attribute("class", "segmentation-layer"));
		segmentationLayer.addAttribute(new Attribute("unselectable", "on"));
		tbody.appendChild(segmentationLayer);
		
		for (TextRange rangePart : rangeParts) {
			
			Element segmentationLayerContent = new Element("td");
			segmentationLayer.appendChild(segmentationLayerContent);
			
			segmentationLayerContent.appendChild(getPresentationContent(rangePart));
		}
		
		return table;
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
	
}
