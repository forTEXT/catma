package de.catma.ui.tagger;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import nu.xom.Text;
import de.catma.core.document.Range;
import de.catma.ui.tagger.client.ui.shared.ContentElementID;

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
		rootDiv.addAttribute(new Attribute(HTMLAttribute.id.name(), ContentElementID.CONTENT.name()));
		htmlDocModel = new Document(rootDiv);
		
		StringBuilder lineBuilder = new StringBuilder();
		int curDocLength = 0;
		int prevDocLength = 0;
		int lineLength = 0;
		int lineId = 0;
		
		while(matcher.find()) {
			if (lineLength + matcher.group().length()>80) {
				Element lineSpan = new Element(HTMLElement.span.name());
				lineSpan.addAttribute(
						new Attribute(HTMLAttribute.id.name(), ContentElementID.LINE.name()+lineId++));
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
				lineSpan.addAttribute(
						new Attribute(HTMLAttribute.id.name(), ContentElementID.LINE.name()+lineId++));
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
			lineSpan.addAttribute(
					new Attribute(HTMLAttribute.id.name(), ContentElementID.LINE.name()+lineId++));
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
	
	@Override
	public String toString() {
		return htmlDocModel.toXML().substring(22).replaceAll("\\Q&amp;_nbsp;\\E", "&nbsp;");
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
