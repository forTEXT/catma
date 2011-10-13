package de.catma.ui.tagger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.Text;

import org.catma.document.Range;

import de.catma.ui.tagger.client.ui.shared.TaggedNode;

public class HTMLWrapper {
	
	private class IndexedRange {
		private Range range;
		private int index;
		
		public IndexedRange(Range range) {
			this(range,0);
		}
		
		public IndexedRange(Range range, int index) {
			super();
			this.range = range;
			this.index = index;
		}
		
		public Range getRange() {
			return range;
		}
	}
	
	private class SegmentElement extends Element {

		private List<IndexedRange> indexedRanges = new ArrayList<HTMLWrapper.IndexedRange>();
		
		public SegmentElement(String name, IndexedRange indexedRange) {
			super(name);
			indexedRanges.add(indexedRange);
		}

		public List<IndexedRange> getIndexedRanges() {
			return indexedRanges;
		}
		
		public int getStartPoint() {
			return (int)indexedRanges.get(0).getRange().getStartPoint();
		}
	}
	
    private static final String LINE_SEPARATOR_PATTERN = 
            "(\r\n|[\n\r\u2028\u2029\u0085])";
	private static final String WORD_PATTERN = 
			"(\\S*)(\\p{Blank}*)" + LINE_SEPARATOR_PATTERN + "?";
	private static final String SOLIDSPACE = "&_nbsp;";

	private Document htmlDocModel;
	
	public HTMLWrapper(String text) {
		buildModel(text);
	}

	private void buildModel(String text) {
		Matcher matcher = Pattern.compile(WORD_PATTERN).matcher(text);
		Element rootDiv = new Element("div");
		rootDiv.addAttribute(new Attribute("id", "raw-text"));
		htmlDocModel = new Document(rootDiv);
		
		StringBuilder lineBuilder = new StringBuilder();
		int curDocLength = 0;
		int prevDocLength = 0;
		int lineLength = 0;
		int lineId = 0;
		
		while(matcher.find()) {
			if (lineLength + matcher.group().length()>80) {
				Element lineSpan = new SegmentElement(
						"span", new IndexedRange(new Range(prevDocLength,curDocLength)));
				prevDocLength = curDocLength;
				lineSpan.addAttribute(new Attribute("id", "LINE"+lineId++));
				lineSpan.appendChild(lineBuilder.toString());
				htmlDocModel.getRootElement().appendChild(lineSpan);
				htmlDocModel.getRootElement().appendChild(new Element("br"));
				lineBuilder = new StringBuilder();
				lineLength = 0;
			}
			if (matcher.group(1) != null) {
				lineBuilder.append(matcher.group(1));
			}
			if ((matcher.group(2) != null) && (!matcher.group(2).isEmpty())){
				lineBuilder.append(getSolidSpace(matcher.group(2).length()));
			}
			if (matcher.group(3) != null) {
				curDocLength+=matcher.group().length();
				Element lineSpan = 
						new SegmentElement(
							"span", new IndexedRange(new Range(prevDocLength,curDocLength)));
				prevDocLength = curDocLength;
				lineSpan.addAttribute(new Attribute("id", "LINE"+lineId++));
				lineSpan.appendChild(lineBuilder.toString());
				htmlDocModel.getRootElement().appendChild(lineSpan);
				htmlDocModel.getRootElement().appendChild(new Element("br"));
				lineBuilder = new StringBuilder();
				lineLength = 0;
			}
			else {
				lineLength += matcher.group().length();
				curDocLength += matcher.group().length();
			}
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
		for(TaggedNode tn : taggedNodes) {
			SegmentElement se = getSegmentByID(tn.getId());

			Range range = new Range(
					se.getStartPoint()+tn.getStartOffset(),
					se.getStartPoint()+tn.getEndOffset());

			
			
			//			result.add(range);

//			List<Range> disjointRanges = se.getRange().getDisjointRanges(range);
//			se.setRange(new Range(range.getStartPoint(), range.getEndPoint()));
//			
//			if (disjointRanges.size() == 2) {
//				Range before = disjointRanges.get(0);
//				String text1 = se.getValue().substring((int)before.getStartPoint(), (int)before.getEndPoint());
//				String text2 = se.getValue().substring((int)range.getStartPoint(), (int)range.getEndPoint());
//				Range after = disjointRanges.get(1);
//				String text3 = se.getValue().substring((int)after.getStartPoint(), (int)after.getEndPoint());
//				((Element)se.getParent()).insertChild(text1, se.getParent().indexOf(se));
//				((Element)se.getParent()).insertChild(text1, se.getParent().indexOf(se)+1);
//				se.replaceChild(se.get, newChild)
//				
//			}
//			else if (disjointRanges.size() == 1) {
//				
//			}
			
//			System.out.println("SE: " + se.getRange() + " " + se.getAttributeValue("id"));
//			System.out.println("tn: " + tn + " " + range);
			
		}
		
		
		return result;
	}

	@Override
	public String toString() {
		return htmlDocModel.toXML().substring(22).replaceAll("\\Q&amp;_nbsp;\\E", "&nbsp;");
	}
	
	private SegmentElement getSegmentByID(String id) {
		Nodes nodes = htmlDocModel.query("//*[@id='"+id+"']");
		if (nodes.size() > 0) {
			return (SegmentElement)nodes.get(0);
		}
		throw new IllegalStateException("unable to find segment with id " + id);
	}

}
