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
import de.catma.document.Range;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.ContentElementID;
import de.catma.ui.client.ui.tagger.shared.TextRange;

/**
 * @author marco.petris@web.de
 *
 */
public class Page {

	private static final String SOLIDSPACE = "&_nbsp;";
	
	private static enum HTMLElement {
		div,
		span, 
		br,
		;
	}
	
	private static enum HTMLAttribute {
		id,
		;
	}

	private int taggerID;
	private int pageStart;
	private int pageEnd;
	private String text;
	private Map<String, ClientTagInstance> relativeTagInstances = 
			new HashMap<String,ClientTagInstance>();
	
	public Page(int taggerID, String text, int pageStart, int pageEnd) {
		this.taggerID = taggerID;
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
		rootDiv.addAttribute(
				new Attribute(
					HTMLAttribute.id.name(), 
					ContentElementID.CONTENT.name()+String.valueOf(taggerID)));
		htmlDocModel = new Document(rootDiv);
		
		StringBuilder lineBuilder = new StringBuilder();
		int lineLength = 0;
		int lineId = 0;
		
		while(matcher.find()) {
			if (lineLength + matcher.group().length()>80) {
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

	public void addRelativeTagInstance(ClientTagInstance relativeTagInstance) {
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
	}
	
	public void removeRelativeTagInstance(String tagInstanceID) {
		this.relativeTagInstances.remove(tagInstanceID);
	}
	
	public Collection<ClientTagInstance> getRelativeTagInstances() {
		return Collections.unmodifiableCollection(this.relativeTagInstances.values());
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
	}

	public ClientTagInstance getRelativeTagInstance(String instanceID) {
		return relativeTagInstances.get(instanceID);
	}
}
