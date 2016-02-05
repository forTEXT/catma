package de.catma.ui.client.ui.tagger.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.Table;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;

import de.catma.ui.client.ui.tagger.shared.AnnotationLayerBuilder;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;

public class Line {
	
	private Element lineElement;
	private String lineId;
	private TextRange textRange;
	private Set<TextRange> tagInstanceTextRanges;
	private Collection<ClientTagInstance> relativeTagIntances;
	private String presentationContent;
	
	public Line(Element lineElement, String lineId, TextRange textRange, Set<TextRange> tagInstanceTextRanges,
			Collection<ClientTagInstance> relativeTagIntances, String presentationContent) {
		super();
		this.lineElement = lineElement;
		this.lineId = lineId;
		this.textRange = textRange;
		this.tagInstanceTextRanges = tagInstanceTextRanges;
		this.relativeTagIntances = relativeTagIntances;
		this.presentationContent = presentationContent;
	}

	private String getPresentationContent() {
		return presentationContent;
	}
	
	public Element createLineElement() {
		
		List<TextRange> rangeParts = new ArrayList<>();
		
		rangeParts.add(new TextRange(this.textRange));
		
		for (TextRange currentTextRange : tagInstanceTextRanges) {
			
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

		Element table = DOM.createTable();
		table.setAttribute("class", "taggerline-table");
		table.setAttribute("id", lineId);
		
		Element tbody = DOM.createTBody();
		table.appendChild(tbody);
		
		Element contentDisplayLayer = DOM.createTR();
		contentDisplayLayer.setAttribute("class", "tagger-display-layer");
		contentDisplayLayer.setAttribute("id", textRange.getStartPos()+"."+textRange.getEndPos());
		
		tbody.appendChild(contentDisplayLayer);
		Element visibleContentLayerContent = DOM.createTD();
		contentDisplayLayer.appendChild(visibleContentLayerContent);
		visibleContentLayerContent.setAttribute(
				"colspan", String.valueOf(rangeParts.size()));
		visibleContentLayerContent.setInnerText(getPresentationContent());
		
		AnnotationLayerBuilder annotationLayerBuilder =
				new AnnotationLayerBuilder(relativeTagIntances, rangeParts);
		
		Table<Integer, TextRange, ClientTagInstance> layerTable = annotationLayerBuilder.getLayerTable();
		int rowCount = layerTable.rowKeySet().size();
		for (int rowIdx = 0; rowIdx<rowCount; rowIdx++) {
			Element annotationLayer = DOM.createTR();
			
			annotationLayer.setAttribute("class", "annotation-layer");
			annotationLayer.setAttribute("unselectable", "on");
			tbody.appendChild(annotationLayer);

			for (TextRange rangePart : rangeParts) {
				
				Element annotationLayerContent = DOM.createTD();
				annotationLayer.appendChild(annotationLayerContent);
				annotationLayerContent.setInnerText(TextRange.NBSP);
				
				ClientTagInstance relativeTagInstance = 
						layerTable.get(rowIdx, rangePart);
				
				if (relativeTagInstance != null) {
					annotationLayerContent.setAttribute("class", "unselected-tag-instance");
					
					annotationLayerContent.setAttribute(
							"id", 
							relativeTagInstance.getInstanceID() 
								+ "."
								+ rangePart.getStartPos()
								+ "."
								+ rangePart.getEndPos());
					annotationLayerContent.setAttribute(
							"style", 
							"background:#"+relativeTagInstance.getColor()
							+";color:#"+relativeTagInstance.getColor());
				}
				else {
					annotationLayerContent.setAttribute(
							"class", "empty-annotation-layer");
				}
			}
		}
		
		Element segmentationLayer = DOM.createTR();

		segmentationLayer.setAttribute("class", "segmentation-layer");
		segmentationLayer.setAttribute("unselectable", "on");
		tbody.appendChild(segmentationLayer);
		
		for (TextRange rangePart : rangeParts) {
			
			Element segmentationLayerContent = DOM.createTD();
			segmentationLayer.appendChild(segmentationLayerContent);
			
			segmentationLayerContent.setInnerText(getPresentationContent(rangePart));
		}
		
		return table;
	}

	private String getPresentationContent(TextRange range) {
		return presentationContent.substring(range.getStartPos()-getLineOffset(), range.getEndPos()-getLineOffset());
	}

	public void addTagInstance(ClientTagInstance clientTagInstance) {
		relativeTagIntances.add(clientTagInstance);
		tagInstanceTextRanges.addAll(clientTagInstance.getRanges());
	}

	public int getLineOffset() {
		return textRange.getStartPos();
	}
	
	public TextRange getTextRange() {
		return textRange;
	}

	public Element getLineElement() {
		return lineElement;
	}

	public void updateLineElement() {
		Element newLineElement = createLineElement();
		lineElement.getParentElement().replaceChild(newLineElement, lineElement);
		this.lineElement = newLineElement;
	}
	
	public int getLineId() {
		return Integer.valueOf(lineId.substring(lineId.indexOf('.')+1));
	}
}
