package de.catma.ui.tagger.pager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import nu.xom.Attribute;
import nu.xom.Element;

public class Line {

	private int lineId;
	private List<LineContent> lineContents;
	private TextRange textRange;
	private int lineLength = 0;

	private Multimap<ClientTagInstance, TextRange> textRangesByRelativeTagInstance;
	

	public Line() {
		lineContents = new ArrayList<>();
		textRangesByRelativeTagInstance = ArrayListMultimap.create();
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

	public void addRelativeTagInstanceTextRange(TextRange tr, ClientTagInstance relativeTagInstance) {
		//FIXME: store range by tiID and create mapping tiID->ti 
		textRangesByRelativeTagInstance.put(relativeTagInstance, tr);

	}
	
	public String toHTML() {
		
		List<TextRange> rangeParts = new ArrayList<>();
		
		rangeParts.add(new TextRange(this.textRange));
		
		for (TextRange currentTextRange : textRangesByRelativeTagInstance.values()) {
			
			for (TextRange rangePart : new TreeSet<TextRange>(rangeParts)) {
				if (rangePart.hasOverlappingRange(currentTextRange)) {
					rangeParts.remove(rangePart);
					rangeParts.add(rangePart.getOverlappingRange(currentTextRange));
					for (TextRange disjointRange : rangePart.getDisjointRanges(currentTextRange)) {
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
		if (rangeParts.size() == 6) {
			System.out.println("now");
		}
		Element table = new Element("table");
		Element tbody = new Element("tbody");
		table.appendChild(tbody);
		
		Element visibleContentLayer = new Element("tr");
		tbody.appendChild(visibleContentLayer);
		Element visibleContentLayerContent = new Element("td");
		visibleContentLayer.appendChild(visibleContentLayerContent);
		visibleContentLayerContent.addAttribute(new Attribute("colspan", String.valueOf(rangeParts.size())));
		visibleContentLayerContent.appendChild(getPresentationContent());
		
		for (ClientTagInstance relativeTagInstance : textRangesByRelativeTagInstance.keys()) {
			Collection<TextRange> textRanges = textRangesByRelativeTagInstance.get(relativeTagInstance);
			Element annotationLayer = new Element("tr");
			
			annotationLayer.addAttribute(new Attribute("style", "line-height:4px"));
			annotationLayer.addAttribute(new Attribute("class", "unselectable"));
			annotationLayer.addAttribute(new Attribute("unselectable", "on"));
			
			tbody.appendChild(annotationLayer);
			
			for (TextRange rangePart : rangeParts) {
				
				Element annotationLayerContent = new Element("td");
				annotationLayer.appendChild(annotationLayerContent);
				annotationLayerContent.appendChild(" ");
				
				if (rangePart.isCoveredBy(textRanges)) {
					annotationLayerContent.addAttribute(
						new Attribute(
								"style", 
								"background:#"+relativeTagInstance.getColor()
								+";foreground:#"+relativeTagInstance.getColor()));
				}
			}
//			System.out.println(tbody.toXML());
		}
		
		Element segmentationLayer = new Element("tr");
		segmentationLayer.addAttribute(new Attribute("style", "line-height:12px"));
		segmentationLayer.addAttribute(new Attribute("class", "unselectable"));
		segmentationLayer.addAttribute(new Attribute("unselectable", "on"));
		tbody.appendChild(segmentationLayer);
		
		for (TextRange rangePart : rangeParts) {
			
			Element segmentationLayerContent = new Element("td");
			segmentationLayer.appendChild(segmentationLayerContent);
			
			segmentationLayerContent.appendChild(getPresentationContent(rangePart));
		}
		
		return table.toXML().replaceAll("\\Q&amp;_nbsp;\\E", "&nbsp;");
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
	
}
