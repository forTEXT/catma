package de.catma.ui.tagger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.Serializer;
import nu.xom.Text;

import org.catma.Pair;
import org.catma.document.Range;

import de.catma.ui.tagger.client.ui.shared.TaggedNode;

public class HTMLWrapper {
	
	private class TextRange extends Text {

		private Range range;
		
		public TextRange(String data, Range range) {
			super(data);
			this.range = range;
		}
		
		public Range getRange() {
			return range;
		}
	}
	
	private static final String LINE_CONTENT_PATTERN = 
			"(\\S+)|(\\p{Blank}+)|(\r\n|[\n\r\u2028\u2029\u0085])"; //(WORDS)|(WHITESPACESEQUENCES)|(LINEBREAK)
	
	private int WORDCHARACTER_GROUP = 1;
	private int WHITESPACE_GROUP = 2;
	private int LINE_SEPARATOR_GROUP = 3;
	
	private static final String SOLIDSPACE = "&_nbsp;";
	
	private static enum HTMLElement {
		div,
		span, br,
		;
	}
	
	private static enum HTMLAttribute {
		id,
		;
	}

	private Document htmlDocModel;
	private String text;
	
	public HTMLWrapper(String text) {
		this.text = text;
		buildModel();
	}
	
	public Document getHtmlDocModel() {
		return htmlDocModel;
	}

	private void buildModel() {
		Matcher matcher = Pattern.compile(LINE_CONTENT_PATTERN).matcher(text);
		Element rootDiv = new Element(HTMLElement.div.name());
		rootDiv.addAttribute(new Attribute(HTMLAttribute.id.name(), "raw-text"));
		htmlDocModel = new Document(rootDiv);
		
		StringBuilder lineBuilder = new StringBuilder();
		int curDocLength = 0;
		int prevDocLength = 0;
		int lineLength = 0;
		int lineId = 0;
		
		while(matcher.find()) {
			if (lineLength + matcher.group().length()>80) {
				Element lineSpan = new Element(HTMLElement.span.name());
				lineSpan.addAttribute(new Attribute(HTMLAttribute.id.name(), "LINE"+lineId++));
				lineSpan.appendChild(
						new TextRange(lineBuilder.toString(), new Range(prevDocLength,curDocLength)));
				htmlDocModel.getRootElement().appendChild(lineSpan);
				htmlDocModel.getRootElement().appendChild(new Element(HTMLElement.br.name()));
				lineBuilder = new StringBuilder();
				lineLength = 0;
				prevDocLength = curDocLength;
			}
			if (matcher.group(WORDCHARACTER_GROUP) != null) {
				lineBuilder.append(matcher.group(WORDCHARACTER_GROUP));
			}
			if ((matcher.group(WHITESPACE_GROUP) != null) && (!matcher.group(WHITESPACE_GROUP).isEmpty())){
				lineBuilder.append(getSolidSpace(matcher.group(WHITESPACE_GROUP).length()));
			}
			if (matcher.group(LINE_SEPARATOR_GROUP) != null) {
				lineBuilder.append(getSolidSpace(matcher.group(LINE_SEPARATOR_GROUP).length()));
				curDocLength+=matcher.group().length();
				Element lineSpan = new Element(HTMLElement.span.name());
				lineSpan.addAttribute(new Attribute(HTMLAttribute.id.name(), "LINE"+lineId++));
				lineSpan.appendChild(new TextRange(lineBuilder.toString(), new Range(prevDocLength,curDocLength)));
				htmlDocModel.getRootElement().appendChild(lineSpan);
				htmlDocModel.getRootElement().appendChild(new Element(HTMLElement.br.name()));
				lineBuilder = new StringBuilder();
				lineLength = 0;
				prevDocLength = curDocLength;
			}
			else {
				lineLength += matcher.group().length();
				curDocLength += matcher.group().length();
			}
		}
		if (lineLength != 0) {
			Element lineSpan = new Element(HTMLElement.span.name());
			lineSpan.addAttribute(new Attribute(HTMLAttribute.id.name(), "LINE"+lineId++));
			lineSpan.appendChild(new TextRange(lineBuilder.toString(), new Range(prevDocLength,curDocLength)));
			htmlDocModel.getRootElement().appendChild(lineSpan);
			htmlDocModel.getRootElement().appendChild(new Element(HTMLElement.br.name()));
		}
	}

	private String getSolidSpace(int count) {
    	StringBuilder builder = new StringBuilder();
    	for (int i=0; i<count;i++) {
    		builder.append(SOLIDSPACE);
    	}
    	return builder.toString();
    }
	
	public List<Range> addTag(String tag, List<TaggedNode> taggedNodes) {
		ArrayList<Range> result = new ArrayList<Range>();
		List<Pair<TaggedNode, TextRange>> taggedTextRanges = new ArrayList<Pair<TaggedNode,TextRange>>();
		
		for(TaggedNode tn : taggedNodes) {
			Element segment = getSegmentByID(tn.getId());
			TextRange tr = (TextRange)segment.getChild(tn.getNodeIndex());
			taggedTextRanges.add(new Pair<TaggedNode, HTMLWrapper.TextRange>(tn,tr));
		}		
		
		for(Pair<TaggedNode, TextRange> taggedTextRange : taggedTextRanges) {
			TaggedNode tn = taggedTextRange.getFirst();
			Element segment = getSegmentByID(tn.getId());
			TextRange tr = taggedTextRange.getSecond();
			
			int referencePoint = (int)tr.getRange().getStartPoint();
			
			Range taggedRange = new Range(
					referencePoint+tn.getStartOffset(),
					referencePoint+tn.getEndOffset());
			
			result.add(taggedRange);
			System.out.println( "added tagged range: " + taggedRange);
			List<Range> disjointRanges = tr.getRange().getDisjointRanges(taggedRange);

			if (disjointRanges.size() == 2) {
				Range before = disjointRanges.get(0);
				segment.insertChild(
						new TextRange(
								convertStandardToHTMLSolidWhitespace(text.substring(
									(int)before.getStartPoint(),
									(int)before.getEndPoint())),
							before), 
						segment.indexOf(tr));
				Range after = disjointRanges.get(1);
				segment.insertChild(
						new TextRange(
								convertStandardToHTMLSolidWhitespace(text.substring(
										(int)after.getStartPoint(),
										(int)after.getEndPoint())),
								after), 
							segment.indexOf(tr)+1);
				
			}
			else if (disjointRanges.size() == 1) {
				
				Range disjointRange = disjointRanges.get(0);
				int insertionPos = 0;
				
				if (disjointRange.startsAfter(taggedRange.getStartPoint())) {
					insertionPos = segment.indexOf(tr)+1;
				}
				else {
					insertionPos = segment.indexOf(tr);
				}
				
				segment.insertChild(
						new TextRange(
							convertStandardToHTMLSolidWhitespace(text.substring(
									(int)disjointRange.getStartPoint(),
									(int)disjointRange.getEndPoint())),
							disjointRange), 
						insertionPos);
			}
			Element newSegment = new Element(HTMLElement.span.name());
			newSegment.addAttribute(new Attribute(HTMLAttribute.id.name(), tn.getTaggedSpanId()));

			newSegment.appendChild(
					new TextRange(
						convertStandardToHTMLSolidWhitespace(text.substring(
								(int)taggedRange.getStartPoint(), 
								(int)taggedRange.getEndPoint())),
						taggedRange));
			
			segment.replaceChild(tr, newSegment);
		}

		return result;
	}

	@Override
	public String toString() {
		return convertEscapedToHTMLSolidWhitespace(htmlDocModel.toXML().substring(22));
	}
	
	private String convertEscapedToHTMLSolidWhitespace(String buf) {
		return buf.replaceAll("\\Q&amp;_nbsp;\\E", "&nbsp;");
	}
	
	private String convertStandardToHTMLSolidWhitespace(String buf) {
		Matcher matcher = Pattern.compile(LINE_CONTENT_PATTERN).matcher(buf);
		StringBuilder result = new StringBuilder();
		
		while(matcher.find()) {
			if (matcher.group(WORDCHARACTER_GROUP) != null) {
				result.append(matcher.group(WORDCHARACTER_GROUP));
			}
			if ((matcher.group(WHITESPACE_GROUP) != null) && (!matcher.group(WHITESPACE_GROUP).isEmpty())){
				result.append(getSolidSpace(matcher.group(WHITESPACE_GROUP).length()));
			}
			if (matcher.group(LINE_SEPARATOR_GROUP) != null) {
				result.append(getSolidSpace(matcher.group(LINE_SEPARATOR_GROUP).length()));
			}
		}
		
		return result.toString();
	}
	
	private Element getSegmentByID(String id) {
		Nodes nodes = htmlDocModel.query("//*[@"+HTMLAttribute.id.name()+"='"+id+"']");
		if (nodes.size() > 0) {
			return (Element)nodes.get(0);
		}
		throw new IllegalStateException("unable to find segment with id " + id);
	}

	public void print() {
		Serializer serializer;
		try {
			serializer = new Serializer( System.out, "UTF-8" );
			serializer.setIndent( 4 );
			serializer.write(htmlDocModel);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
