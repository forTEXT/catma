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
package de.catma.ui.module.annotate.pager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.catma.document.Range;
import de.catma.document.comment.Comment;
import de.catma.tag.TagDefinition;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.ContentElementID;
import de.catma.ui.client.ui.tagger.shared.TextRange;
import nu.xom.Attribute;
import nu.xom.Element;

/**
 * @author marco.petris@web.de
 *
 */
public class Page {

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
		clazz("class")
		;
	
		private String attributeName;
		
		
		private HTMLAttribute(String attributeName) {
			this.attributeName = attributeName;
		}

		private HTMLAttribute() {
		}
		
		public String getAttributeName() {
			return attributeName == null?name():attributeName;
		}
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
	private boolean rightToLeftWriting;
	private ArrayList<Line> lines;
	private Element pageDiv;
	private Set<Comment> relativeComments;
	
	public Page(int taggerID, String text, int pageStart, int pageEnd, int approxMaxLineLength, boolean rightToLeftWriting) {
		this.taggerID = taggerID;
		this.pageStart = pageStart;
		this.pageEnd = pageEnd;
		this.approxMaxLineLength = approxMaxLineLength;
		this.text = text;
		this.rightToLeftWriting = rightToLeftWriting;
		this.relativeComments = new HashSet<Comment>();
		buildLines();
	}
	
	@Override
	public String toString() {
		return "Page["+pageStart+","+pageEnd+"]\n"+text; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	
	private void buildLines() {
		this.lines = new ArrayList<>();
		
		Matcher matcher = Pattern.compile(Pager.LINE_CONTENT_PATTERN).matcher(text);
		
		Line currentLine = new Line(rightToLeftWriting);
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
				currentLine = new Line(rightToLeftWriting);
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
				currentLine = new Line(rightToLeftWriting);
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
		
	}

	public String toHTML() {
		if (pageDiv == null) {
			pageDiv = new Element(HTMLElement.div.name());
			if (rightToLeftWriting) {
				pageDiv.addAttribute(
					new Attribute(HTMLAttribute.dir.name(), HTMLAttributeValue.rtl.name()));
				pageDiv.addAttribute(
					new Attribute(HTMLAttribute.align.name(), HTMLAttributeValue.right.name()));
			}
			pageDiv.addAttribute(
					new Attribute(
						HTMLAttribute.id.name(), 
						ContentElementID.CONTENT.name()+String.valueOf(taggerID)));
			pageDiv.addAttribute(new Attribute(
					HTMLAttribute.clazz.getAttributeName(),
					"tagger-editor-content"));
			for (Line line : lines) {
				pageDiv.appendChild(line.toHTML());
			}
		}
		return pageDiv.toXML();
	}

	public void addRelativeTagInstance(ClientTagInstance relativeTagInstance) {
		
		this.pageDiv = null; // page needs rebuild
		
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
		pageDiv = null; // page needs rebuild
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
		return hasOverlappingRange(absoluteTagInstance.getRanges());
	}
	
	public boolean hasOverlappingRange(List<TextRange> absoluteTextRanges) {
		TextRange pageRange = new TextRange(this.pageStart, this.pageEnd);
		
		if (!absoluteTextRanges.isEmpty()) {
			for (TextRange tr : absoluteTextRanges) {
				if (pageRange.hasOverlappingRange(tr)) {
					return true;
				}
			}
		}
		return false;
	}

	public void clearRelativeTagInstances() {
		pageDiv = null; // page needs rebuild
		
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
		pageDiv = null; // page needs rebuild
		
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

	public List<String> getTagInstanceIDs(String instancePartID, String lineID) {
		try {
			return Collections.singletonList(ClientTagInstance.getTagInstanceIDFromPartId(instancePartID));
		}
		catch (IndexOutOfBoundsException | NullPointerException e) {
			Logger.getLogger(Page.class.getName()).log(
				Level.SEVERE, "No such lineID: " + lineID + " with tagInstanceID: " + instancePartID , e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return Collections.emptyList();
	}

	public Range getOverlappingRange(Range absoluteTextRange) {
		return new Range(
				this.pageStart, this.pageEnd).getOverlappingRange(absoluteTextRange);
	}

	/**
	 * @param highlightedAbsoluteRange
	 * @return the ID of the first affected {@link Line}
	 */
	public int addHighlight(Range highlightedAbsoluteRange) {
		pageDiv = null;
		Line firstLine = null;
		
		TextRange highlightedRelativeRange = getRelativeRangeFor(highlightedAbsoluteRange);
		
		for (Line line : lines) {
			TextRange overlappingRange = line.getOverlappingRange(highlightedRelativeRange);
			if (overlappingRange != null) {
				line.addHighlight(overlappingRange);
				firstLine = (firstLine==null)?line:firstLine;
			}
		}
		
		if (firstLine != null) {
			return firstLine.getLineId();
		}
		return -1;
		
	}

	public boolean isDirty() {
		return pageDiv==null;
	}

	public void removeHighlights() {
		for (Line line : lines) {
			if (line.hasHighlights()) {
				line.removeHighlights();
				pageDiv = null;
			}
		}
	}

	public boolean contains(String annotationId) {
		return this.relativeTagInstances.containsKey(annotationId);
	}

	public boolean hasLine(int lineId) {
		return lines.stream().filter(line -> line.getLineId() == lineId).findAny().isPresent();
	}

	public void addAbsoluteComment(Comment absoluteComment) {
		Comment relativeComment = new Comment(absoluteComment, pageStart*-1);
		relativeComments.add(relativeComment);
	}

	public Optional<Comment> getRelativeComment(String uuid) {
		return relativeComments.stream().filter(comment -> comment.getUuid().equals(uuid)).findFirst();
	}

	public Collection<Comment> getRelativeComments() {
		return Collections.unmodifiableCollection(relativeComments);
	}

	public void removeAbsoluteComment(Comment comment) {
		
		Iterator<Comment> relCommentIterator= relativeComments.iterator();
		while(relCommentIterator.hasNext()) {
			if (relCommentIterator.next().getUuid().equals(comment.getUuid())) {
				relCommentIterator.remove();
				break;
			}
		}
		
	}
}

