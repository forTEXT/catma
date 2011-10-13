package de.catma.ui.tagger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;

import org.catma.document.Range;

import de.catma.ui.tagger.client.ui.shared.TaggedNode;

public class HTMLWrapper {
	
    private static final String LINE_SEPARATOR_PATTERN = 
            "(\r\n|[\n\r\u2028\u2029\u0085])";
	private static final String WORD_PATTERN = 
			"(\\p{Blank}*)(\\S*)(\\p{Blank}*)" + LINE_SEPARATOR_PATTERN + "?";
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
		
		StringBuilder builder = new StringBuilder();
		StringBuilder lineBuilder = new StringBuilder();
		int lineLength = 0;
		int lineId=0;
		
		while(matcher.find()) {
			if (lineLength + matcher.group().length()>80) {
				Element lineSpan = new Element("span");
				lineSpan.addAttribute(new Attribute("id", "LINE"+lineId++));
				lineSpan.appendChild(lineBuilder.toString());
				htmlDocModel.getRootElement().appendChild(lineSpan);
				htmlDocModel.getRootElement().appendChild(new Element("br"));
				lineBuilder = new StringBuilder();
				lineLength = 0;
			}
			if (matcher.group(1) != null) {
				lineBuilder.append(getSolidSpace(matcher.group(1).length()));
			}
			if (matcher.group(2) != null) {
				lineBuilder.append(matcher.group(2));
			}
			if (matcher.group(3) != null) {
				lineBuilder.append(getSolidSpace(matcher.group(3).length()));
			}
			if (matcher.group(4) != null) {
				Element lineSpan = new Element("span");
				lineSpan.addAttribute(new Attribute("id", "LINE"+lineId++));
				lineSpan.appendChild(lineBuilder.toString());
				htmlDocModel.getRootElement().appendChild(lineSpan);
				htmlDocModel.getRootElement().appendChild(new Element("br"));
				lineBuilder = new StringBuilder();
				lineLength = 0;
			}
			else {
				lineLength += matcher.group().length();
			}
		}
		text = builder.toString();
	}

	private String getSolidSpace(int count) {
    	StringBuilder builder = new StringBuilder();
    	for (int i=0; i<count;i++) {
    		builder.append(SOLIDSPACE);
    	}
    	return builder.toString();
    }
	
	public Range addTag(String tag, List<TaggedNode> taggedNodes) {
		
		
		
		
		return null;
	}

	@Override
	public String toString() {
		return htmlDocModel.toXML().substring(22).replaceAll("\\Q&amp;_nbsp;\\E", "&nbsp;");
	}

}
