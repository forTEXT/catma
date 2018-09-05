package de.catma.ui.client.ui.tagger.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;

import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;

public class LineNodeToLineConverter {

	private String lineId;
	private TextRange textRange;
	private Set<TextRange> tagInstanceTextRanges;
	private Set<TextRange> highlightedTextRanges;
	private Map<String,ClientTagInstance> absoluteTagIntancesByID;
	private String presentationContent;
	private Line line;
	
	public LineNodeToLineConverter(Element lineElement) {
		tagInstanceTextRanges = new HashSet<>();
		highlightedTextRanges = new HashSet<>();
		absoluteTagIntancesByID = new HashMap<>();
		makeLineFromLineNode(lineElement);
	}
	
	private void makeLineFromLineNode(Element lineElement) {
		lineId = lineElement.getId();
		Element lineBodyElement = lineElement.getFirstChildElement();
		
		for (int layerIdx = 0; layerIdx < lineBodyElement.getChildCount(); layerIdx++) {
			Node layerNode = lineBodyElement.getChild(layerIdx);
			if (Element.is(layerNode)) {
				Element layerElement = Element.as(layerNode);
				
				if (layerElement.hasClassName("tagger-display-layer")) {
					handleDisplayLayer(layerElement);
				}
				else if (layerElement.hasClassName("annotation-layer")) {
					handleAnnotationLayer(layerElement);
				}
				else if (layerElement.hasClassName("highlight-layer")) {
					handleHighlightLayer(layerElement);
				}
			}
		}
		
		for (ClientTagInstance tagInstance : absoluteTagIntancesByID.values()) {
			tagInstanceTextRanges.addAll(tagInstance.getRanges());
		}
		
		line = new Line(
			lineElement,
			lineId, textRange, tagInstanceTextRanges, highlightedTextRanges,
			new ArrayList<ClientTagInstance>(absoluteTagIntancesByID.values()), 
			presentationContent);
	}

	private void handleHighlightLayer(Element layerElement) {
		for (int highlightedSegmentIdx = 0; 
				highlightedSegmentIdx<layerElement.getChildCount(); highlightedSegmentIdx++) {
			Node hightlightedSegmentNode = layerElement.getChild(highlightedSegmentIdx);
			if (Element.is(hightlightedSegmentNode)) {
				Element highlightedSegmentElement = Element.as(hightlightedSegmentNode);
				
				if (highlightedSegmentElement.hasClassName("highlighted-content")) {
					// id is expected to have the form h.startoffset.endoffset
					String[] rangePositions = highlightedSegmentElement.getId().split("\\.");
					TextRange highlightedTextRange = 
						new TextRange(
							Integer.valueOf(rangePositions[1]),
							Integer.valueOf(rangePositions[2]));
					highlightedTextRanges.add(highlightedTextRange);
				}
			}
		}
	}

	private void handleAnnotationLayer(Element layerElement) {
		for (int annotationSegmentIdx = 0; 
				annotationSegmentIdx<layerElement.getChildCount(); annotationSegmentIdx++) {
			Node annotationSegmentNode = layerElement.getChild(annotationSegmentIdx);
			if (Element.is(annotationSegmentNode)) {
				Element annotationSegmentElement = Element.as(annotationSegmentNode);
				
				if (annotationSegmentElement.getId() != null &&
						annotationSegmentElement.getId().startsWith("CATMA_")) {
					
					String annotationSegmentId = annotationSegmentElement.getId();
					Style annotationSegmentStyle = annotationSegmentElement.getStyle();
					String annotationColor = annotationSegmentStyle.getBackgroundColor();
					handleAnnotationSegmment(annotationSegmentId, annotationColor);
				}
			}
		}
	}

	private void handleAnnotationSegmment(String annotationSegmentId, String annotationColor) {
		annotationColor = getConvertedColor(annotationColor);
		
		String annotationId = ClientTagInstance.getTagInstanceIDFromPartId(annotationSegmentId);
		TextRange textRange = ClientTagInstance.getTextRangeFromPartId(annotationSegmentId);
		ClientTagInstance tagInstance = absoluteTagIntancesByID.get(annotationId);
		if (tagInstance != null) {
			TreeSet<TextRange> sortedRanges = new TreeSet<>(tagInstance.getRanges());
			sortedRanges.add(textRange);
			tagInstance = new ClientTagInstance(null, annotationId, annotationColor, ClientTagInstance.mergeRanges(sortedRanges));
		}
		else {
			List<TextRange> textRanges = new ArrayList<>();
			textRanges.add(textRange);
			tagInstance = new ClientTagInstance(null, annotationId, annotationColor, textRanges);
		}
		
		absoluteTagIntancesByID.put(annotationId, tagInstance);
	}

	private String getConvertedColor(String annotationColor) {
		if (annotationColor.startsWith("rgb")) {
			String[] colorStrings = annotationColor.substring(4, annotationColor.length()-1).split(",");
			
			int red = Integer.valueOf(colorStrings[0].trim());
			int green = Integer.valueOf(colorStrings[1].trim());
			int blue = Integer.valueOf(colorStrings[2].trim());
			return fillUp(Integer.toHexString(red).toUpperCase()) 
					+ fillUp(Integer.toHexString(green).toUpperCase()) 
					+ fillUp(Integer.toHexString(blue).toUpperCase());
		}
		return annotationColor;
	}
	
	private String fillUp(String hexString) {
		if (hexString.length() < 2) {
			return "0"+hexString;
		}
		
		return hexString;
	}

	private void handleDisplayLayer(Element layerElement) {
		textRange = getTextRangeFromDisplayLayer(layerElement);
		presentationContent = layerElement.getFirstChildElement().getInnerText();
	}
	
	public static TextRange getTextRangeFromDisplayLayer(Element layerElement) {
		String[] rangePositions = layerElement.getId().split("\\.");
		return new TextRange(Integer.valueOf(rangePositions[0]),Integer.valueOf(rangePositions[1]));
	}
	
	public Line getLine() {
		return line;
	}

}
