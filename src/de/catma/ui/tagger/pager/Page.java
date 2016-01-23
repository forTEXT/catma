/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2012  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */   
package de.catma.ui.tagger.pager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import nu.xom.Text;
import de.catma.document.Range;
import de.catma.tag.TagDefinition;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.ContentElementID;
import de.catma.ui.client.ui.tagger.shared.TextRange;

/**
 * @author marco.petris@web.de
 *
 */
public class Page {

	static final String SOLIDSPACE = "&_nbsp;";
	
	private static enum HTMLElement {
		div,
		span, 
		br,
		;
	}
	
	private static enum HTMLAttribute {
		id,
		dir,
		align,
		;
	}
	
	private static enum HTMLAttributeValue {
		rtl,
		right,
		;
	}
	

	private int taggerID;
	private int pageStart;
	private int pageEnd;
	private int approxMaxLineLength;
	private String text;
	private Map<String, ClientTagInstance> relativeTagInstances = 
			new HashMap<String,ClientTagInstance>();
	private int lineCount;
	private boolean rightToLeftLanguage;
	private ArrayList<Line> lines;
	
	public Page(int taggerID, String text, int pageStart, int pageEnd, int approxMaxLineLength, boolean rightToLeftLanguage) {
		this.taggerID = taggerID;
		this.pageStart = pageStart;
		this.pageEnd = pageEnd;
		this.approxMaxLineLength = approxMaxLineLength;
		this.text = text;
		this.rightToLeftLanguage = rightToLeftLanguage;
		buildLines();
	}
	
	@Override
	public String toString() {
		return "Page["+pageStart+","+pageEnd+"]\n"+text;
	}
	
	private Document htmlDocModel;
	
	private void buildLines() {
		this.lines = new ArrayList<>();
		
		Matcher matcher = Pattern.compile(Pager.LINE_CONTENT_PATTERN).matcher(text);
		
		Line currentLine = new Line();
		int lineLength = 0;
		int lineId = 0;
		int pageOffset = 0;
		
		while(matcher.find()) {
			if (lineLength + matcher.group().length()>approxMaxLineLength) {
				int lineStart = pageOffset;
				int lineEnd = pageOffset+lineLength; 
				
				currentLine.setLineId(lineId++);
				currentLine.setTextRange(new TextRange(lineStart, lineEnd));
				
				lines.add(currentLine);
				
				pageOffset += lineEnd-lineStart;
				currentLine = new Line();
				lineLength = 0;
			}
			if (matcher.group(Pager.WORDCHARACTER_GROUP) != null) {
				currentLine.addCharacterContent(matcher.group(Pager.WORDCHARACTER_GROUP));
			}
			if ((matcher.group(Pager.WHITESPACE_GROUP) != null) && (!matcher.group(Pager.WHITESPACE_GROUP).isEmpty())){
				currentLine.addWhitespaceContent(matcher.group(Pager.WHITESPACE_GROUP));
			}
			if (matcher.group(Pager.LINE_SEPARATOR_GROUP) != null) {
				lineLength += matcher.group(Pager.LINE_SEPARATOR_GROUP).length();
				currentLine.addLineSeparatorContent(matcher.group(Pager.LINE_SEPARATOR_GROUP));
				int lineStart = pageOffset;
				int lineEnd = pageOffset+lineLength; 

				currentLine.setLineId(lineId++);
				currentLine.setTextRange(new TextRange(lineStart, lineEnd));

				lines.add(currentLine);

				pageOffset += lineEnd-lineStart;
				currentLine = new Line();
				lineLength = 0;
			}
			else {
				lineLength += matcher.group().length();
			}
		}
		if (lineLength != 0) {
			int lineStart = pageOffset;
			int lineEnd = pageOffset+lineLength;
			
			currentLine.setLineId(lineId++);
			currentLine.setTextRange(new TextRange(lineStart, lineEnd));
			
			
			lines.add(currentLine);
		}
		
		lineCount = lineId;
		
//		for (Line line : lines) {
//			System.out.println(line);
//		}
	}

	
	private void buildModel() {
		Matcher matcher = Pattern.compile(Pager.LINE_CONTENT_PATTERN).matcher(text);
		Element rootDiv = new Element(HTMLElement.div.name());
		if (rightToLeftLanguage) {
			rootDiv.addAttribute(new Attribute(HTMLAttribute.dir.name(), HTMLAttributeValue.rtl.name()));
			rootDiv.addAttribute(new Attribute(HTMLAttribute.align.name(), HTMLAttributeValue.right.name()));
		}
		rootDiv.addAttribute(
				new Attribute(
					HTMLAttribute.id.name(), 
					ContentElementID.CONTENT.name()+String.valueOf(taggerID)));
		htmlDocModel = new Document(rootDiv);
		
		StringBuilder lineBuilder = new StringBuilder();
		int lineLength = 0;
		int lineId = 0;
		
		while(matcher.find()) {
			if (lineLength + matcher.group().length()>approxMaxLineLength) {
				Element lineSpan = new Element(HTMLElement.span.name());
				lineSpan.addAttribute(
						new Attribute(
								HTMLAttribute.id.name(), 
								ContentElementID.LINE.name()+taggerID+lineId++));
				lineSpan.appendChild(
						new Text(lineBuilder.toString()));
				htmlDocModel.getRootElement().appendChild(lineSpan);
				htmlDocModel.getRootElement().appendChild(new Element(HTMLElement.br.name()));
				lineBuilder = new StringBuilder();
				lineLength = 0;
			}
			if (matcher.group(Pager.WORDCHARACTER_GROUP) != null) {
				lineBuilder.append(matcher.group(Pager.WORDCHARACTER_GROUP));
			}
			if ((matcher.group(Pager.WHITESPACE_GROUP) != null) && (!matcher.group(Pager.WHITESPACE_GROUP).isEmpty())){
				lineBuilder.append(getSolidSpace(matcher.group(Pager.WHITESPACE_GROUP).length()));
			}
			if (matcher.group(Pager.LINE_SEPARATOR_GROUP) != null) {
				lineBuilder.append(getSolidSpace(matcher.group(Pager.LINE_SEPARATOR_GROUP).length()));
				Element lineSpan = new Element(HTMLElement.span.name());
				lineSpan.addAttribute(
						new Attribute(
								HTMLAttribute.id.name(), 
								ContentElementID.LINE.name()+taggerID+lineId++));
				lineSpan.appendChild(new Text(lineBuilder.toString()));
				htmlDocModel.getRootElement().appendChild(lineSpan);
				htmlDocModel.getRootElement().appendChild(new Element(HTMLElement.br.name()));
				lineBuilder = new StringBuilder();
				lineLength = 0;
			}
			else {
				lineLength += matcher.group().length();
			}
		}
		if (lineLength != 0) {
			Element lineSpan = new Element(HTMLElement.span.name());
			lineSpan.addAttribute(
					new Attribute(
						HTMLAttribute.id.name(), 
						ContentElementID.LINE.name()+taggerID+lineId++));
			lineSpan.appendChild(new Text(lineBuilder.toString()));
			htmlDocModel.getRootElement().appendChild(lineSpan);
			htmlDocModel.getRootElement().appendChild(new Element(HTMLElement.br.name()));
		}
		
		lineCount = lineId;
	}

	private String getSolidSpace(int count) {
    	StringBuilder builder = new StringBuilder();
    	for (int i=0; i<count;i++) {
    		builder.append(SOLIDSPACE);
    	}
    	return builder.toString();
    }
	
	public String toHTML() {
		Element rootDiv = new Element(HTMLElement.div.name());
		if (rightToLeftLanguage) {
			rootDiv.addAttribute(new Attribute(HTMLAttribute.dir.name(), HTMLAttributeValue.rtl.name()));
			rootDiv.addAttribute(new Attribute(HTMLAttribute.align.name(), HTMLAttributeValue.right.name()));
		}
		rootDiv.addAttribute(
				new Attribute(
					HTMLAttribute.id.name(), 
					ContentElementID.CONTENT.name()+String.valueOf(taggerID)));
		
		TagInstanceTextRangeIdHandler tagInstanceTextRangeIdHandler = new TagInstanceTextRangeIdHandler(); 
		for (Line line : lines) {
			rootDiv.appendChild(line.toHTML(tagInstanceTextRangeIdHandler));
		}
		
		return rootDiv.toXML().replaceAll("\\Q&amp;_nbsp;\\E", "&nbsp;");
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

	public void addRelativeTagInstance(ClientTagInstance relativeTagInstance) {
		// for relative TagInstances the amount of text ranges varies depending 
		// on the current page and the current page size
		// so we need to append those that are not present
		if (this.relativeTagInstances.containsKey(
				relativeTagInstance.getInstanceID())) {
			this.relativeTagInstances.get(
					relativeTagInstance.getInstanceID()).addRanges(
							relativeTagInstance.getRanges());
		}
		else {
			this.relativeTagInstances.put(
					relativeTagInstance.getInstanceID(),relativeTagInstance);
		}
		
		addRelativeTagInstanceToLine(relativeTagInstance);
	}
	
	private void addRelativeTagInstanceToLine(ClientTagInstance relativeTagInstance) {
		for (TextRange tr : relativeTagInstance.getRanges()) {
			for (Line line : lines) {
				if (line.containsTextRange(tr)) {
					line.addRelativeTagInstanceTextRange(tr, relativeTagInstance);
				}
			}
		}
		
	}


	public void removeRelativeTagInstance(String tagInstanceID) {
		this.relativeTagInstances.remove(tagInstanceID);
		removeRelativeTagInstanceFromLine(tagInstanceID);
	}
	
	private void removeRelativeTagInstanceFromLine(String tagInstanceID) {
		for (Line line : lines) {
			line.removeRelativeTagInstance(tagInstanceID);
		}
	}

	public Collection<ClientTagInstance> getRelativeTagInstances() {
		return Collections.unmodifiableCollection(this.relativeTagInstances.values());
	}

	public Collection<ClientTagInstance> getAbsoluteTagInstances() {
		Collection<ClientTagInstance> absoluteTagInstances = new ArrayList<ClientTagInstance>();
		for (ClientTagInstance cti : getRelativeTagInstances()) {
			absoluteTagInstances.add(getAbsoluteTagInstance(cti));
		}
		return absoluteTagInstances;
	}
	
	public ClientTagInstance getAbsoluteTagInstance(ClientTagInstance tagInstance) {
		return new ClientTagInstance(tagInstance, pageStart);
	}

	public void addAbsoluteTagInstance(ClientTagInstance ti) {
		ClientTagInstance relativeInstance = 
				new ClientTagInstance(ti, pageStart*(-1), pageStart, pageEnd);
		addRelativeTagInstance(relativeInstance);
	}

	public boolean hasOverlappingRange(ClientTagInstance absoluteTagInstance) {
		if (!absoluteTagInstance.getRanges().isEmpty()) {
			for (TextRange tr : absoluteTagInstance.getRanges()) {
				if (new Range(
					this.pageStart, this.pageEnd).hasOverlappingRange(
							new Range(tr.getStartPos(), tr.getEndPos()))) {
					return true;
				}
			}
		}
		return false;
	}

	public void clearRelativeTagInstances() {
		relativeTagInstances.clear();
		for (Line line : lines) {
			line.clearRelativeTagInstanes();
		}
	}

	public ClientTagInstance getRelativeTagInstance(String instanceID) {
		return relativeTagInstances.get(instanceID);
	}

	public boolean hasPoint(int startPoint) {
		return (this.pageStart <= startPoint) && (this.pageEnd >= startPoint);
	}

	public TextRange getRelativeRangeFor(Range absoluteTextRange) {
		return new TextRange(
				absoluteTextRange.getStartPoint()-pageStart, 
				absoluteTextRange.getEndPoint()-pageStart);
	}

	public void removeTagInstances(Set<TagDefinition> tagDefinitions) {
		Set<String> tagDefUUIds = new HashSet<String>();
		for (TagDefinition td : tagDefinitions) {
			tagDefUUIds.add(td.getUuid());
		}
		Iterator<Map.Entry<String, ClientTagInstance>> iterator = 
				relativeTagInstances.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, ClientTagInstance> entry = 
					iterator.next();
			if (tagDefUUIds.contains(entry.getValue().getTagDefinitionID())) {
				iterator.remove();
				removeRelativeTagInstanceFromLine(entry.getKey());
			}
		}
	}
	
	public int getLineCount() {
		return lineCount;
	}
	
	public int getPageEnd() {
		return pageEnd;
	}
	
	public int getPageStart() {
		return pageStart;
	}
}
