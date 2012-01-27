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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import nu.xom.Text;
import de.catma.ui.client.ui.tagger.shared.ContentElementID;
import de.catma.ui.client.ui.tagger.shared.TagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;

/**
 * @author marco.petris@web.de
 *
 */
public class Page {

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

	private int pageStart;
	private int pageEnd;
	private String text;
	private Map<String, TagInstance> tagInstances = new HashMap<String,TagInstance>();
	
	public Page(String text, int pageStart, int pageEnd) {
		this.pageStart = pageStart;
		this.pageEnd = pageEnd;
		this.text = text;
	}
	
	@Override
	public String toString() {
		return "Page["+pageStart+","+pageEnd+"]\n"+text;
	}
	
	
	private Document htmlDocModel;
	
	private void buildModel() {
		Matcher matcher = Pattern.compile(Pager.LINE_CONTENT_PATTERN).matcher(text);
		Element rootDiv = new Element(HTMLElement.div.name());
		rootDiv.addAttribute(new Attribute(HTMLAttribute.id.name(), ContentElementID.CONTENT.name()));
		htmlDocModel = new Document(rootDiv);
		
		StringBuilder lineBuilder = new StringBuilder();
		int lineLength = 0;
		int lineId = 0;
		
		while(matcher.find()) {
			if (lineLength + matcher.group().length()>80) {
				Element lineSpan = new Element(HTMLElement.span.name());
				lineSpan.addAttribute(
						new Attribute(HTMLAttribute.id.name(), ContentElementID.LINE.name()+lineId++));
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
						new Attribute(HTMLAttribute.id.name(), ContentElementID.LINE.name()+lineId++));
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
					new Attribute(HTMLAttribute.id.name(), ContentElementID.LINE.name()+lineId++));
			lineSpan.appendChild(new Text(lineBuilder.toString()));
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
	
	public String toHTML() {
		if (htmlDocModel == null) {
			buildModel();
		}
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

	public void addTagInstance(TagInstance tagInstance) {
		this.tagInstances.put(tagInstance.getInstanceID(), tagInstance);
	}
	
	public void removeTagInstance(String tagInstanceID) {
		this.tagInstances.remove(tagInstanceID);
	}
	
	public Collection<TagInstance> getTagInstances() {
		return Collections.unmodifiableCollection(this.tagInstances.values());
	}

	public TagInstance getAbsoluteTagInstance(TagInstance tagInstance) {
		return new TagInstance(tagInstance, pageStart);
	}

	public void addAbsoluteTagInstance(TagInstance ti) {
		TagInstance relativeInstance = new TagInstance(ti, pageStart*(-1));
		System.out.println(relativeInstance);
		addTagInstance(relativeInstance);
		
	}

	public boolean includes(TagInstance ti) {
		if (ti.getRanges().size()>0) {
			TextRange tr = ti.getRanges().get(0);
			if ((tr.getStartPos()>=this.pageStart) && (tr.getEndPos()<=this.pageEnd)) {
				return true;
			}
		}
		return false;
	}

	public void clearTagInstances() {
		tagInstances.clear();
	}
}
