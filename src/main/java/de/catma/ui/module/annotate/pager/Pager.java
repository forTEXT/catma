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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import de.catma.document.Range;
import de.catma.document.comment.Comment;
import de.catma.tag.TagDefinition;
import de.catma.ui.client.ui.tagger.shared.ClientComment;
import de.catma.ui.client.ui.tagger.shared.ClientTagInstance;
import de.catma.ui.client.ui.tagger.shared.TextRange;

/**
 * @author marco.petris@web.de
 *
 */
public class Pager implements Iterable<Page> {
	
	public static interface PagerListener {
		public void textChanged();
	}
	
	static final String LINE_CONTENT_PATTERN = 
			"(\\S+)|(\\p{Blank}+)|(\r\n|[\n\r\u2028\u2029\u0085])"; //$NON-NLS-1$
	
	static int WORDCHARACTER_GROUP = 1;
	static int WHITESPACE_GROUP = 2;
	static int LINE_SEPARATOR_GROUP = 3;


	private ArrayList<Page> pages;
	private int currentPageIndex=0;

	private int approxMaxLineLength;
	private int maxPageLengthInLines;
	private PagerListener pagerListener;
	private Long checksum = null;
	private int taggerID;
	private int totalLineCount = 0;

	private boolean rightToLeftWriting;
	
	public Pager(int taggerID, int approxMaxLineLength, int maxPageLengthInLines, boolean rightToLeftWriting) {
		pages = new ArrayList<Page>();
		this.taggerID = taggerID;
		this.approxMaxLineLength = approxMaxLineLength;
		this.maxPageLengthInLines = maxPageLengthInLines;
		this.rightToLeftWriting = rightToLeftWriting;
	}
	
	public void setText(String text, Collection<Comment> comments) {
		if (!matchChecksum(text)) {
			currentPageIndex = 0;
			pages.clear();
			buildPages(text);
			this.setComments(comments);
			if (pagerListener != null) {
				pagerListener.textChanged();
			}
		}
		
	}
	
	public boolean hasPages() {
		return !pages.isEmpty();
	}

	private boolean matchChecksum(String text) {
		
		CRC32 crc32 = new CRC32();
		crc32.update(text.getBytes());
		if ( (checksum != null) && (crc32.getValue() == checksum)) {
			return true;
		}
		
		checksum = crc32.getValue();

		return false;
	}

	private void buildPages(String text) {
		
		//TODO: always break after blanks and directly before the next word
		
		Matcher matcher = Pattern.compile(LINE_CONTENT_PATTERN).matcher(text);

		int pageStart = 0;
		int pageEnd = 0;
		int pageLines = 0;
		
		int lineLength = 0;
		
		totalLineCount = 0;
		
		while(matcher.find()) {
			if (lineLength + matcher.group().length()>approxMaxLineLength) {
				pageLines++;
				totalLineCount++;
				pageEnd+=lineLength;
				lineLength = 0;
			}			
			
			if (pageLines >= maxPageLengthInLines) {
				pages.add(new Page(
						taggerID, 
						text.substring(pageStart, pageEnd), 
						pageStart, pageEnd, approxMaxLineLength,
						rightToLeftWriting));
				pageLines = 0;
				pageStart = pageEnd;
			}

			lineLength += matcher.group().length();
			
			if (matcher.group(LINE_SEPARATOR_GROUP) != null) {
				pageLines++;
				totalLineCount++;
				pageEnd+=lineLength;
				lineLength = 0;
			}
		}
		
		if (lineLength != 0) {
			pageEnd+=lineLength;
			pageLines++;
			totalLineCount++;
		}
		
		if (pageLines != 0) {
			pages.add(
				new Page(
					taggerID, 
					text.substring(pageStart, pageEnd), 
					pageStart, pageEnd, approxMaxLineLength,
					rightToLeftWriting));
		}		
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (Page page : pages) {
			builder.append(page);
		}
		return builder.toString();
	}

	public Page getCurrentPage() {
		return pages.get(currentPageIndex);
	}
	
	public int getCurrentPageNumber() {
		return currentPageIndex+1;
	}
	
	public Page getPage(int pageNumber) {
		int index = pageNumber-1;
		if (index < 0) {
			index = 0;
		}
		else if (index >= pages.size()) {
			index = pages.size()-1;
		}
		currentPageIndex = index;
		return pages.get(index);
	}

	public boolean isEmpty() {
		return pages.isEmpty();
	}
	
	public int getLastPageNumber() {
		return pages.size();
	}
	
	public void setPagerListener(PagerListener pagerListener) {
		this.pagerListener = pagerListener;
	}

	public List<Page> getPagesForAbsoluteTagInstance(ClientTagInstance absoluteTagInstance) {
		List<Page> result = new ArrayList<Page>();
		
		for (Page p : pages) {
			if (p.hasOverlappingRange(absoluteTagInstance)) {
				result.add(p);
			}
		}
		
		return result;
	}
	
	public List<Page> getPagesForAbsoluteTextRanges(List<TextRange> textRanges) {
		List<Page> result = new ArrayList<Page>();
		
		for (Page p : pages) {
			if (p.hasOverlappingRange(textRanges)) {
				result.add(p);
			}
		}
		
		return result;
	}
	
	public List<ClientTagInstance> getAbsoluteTagInstances() {
		List<ClientTagInstance> absoluteTagInstances = 
				new ArrayList<ClientTagInstance>();
		
		for (Page p : pages) {
			absoluteTagInstances.addAll(p.getAbsoluteTagInstances());
		}
		
		return absoluteTagInstances;
	}
	
	public int getStartPageNumberFor(Range range) {
		return getPageNumberFor(range.getStartPoint());
	}

	public int getPageNumberFor(int point) {
		
		for (Page p: pages) {
			if (p.hasPoint(point)) {
				return pages.indexOf(p)+1;
			}
		}
		return -1;
	}
	
	public Iterator<Page> iterator() {
		return Collections.unmodifiableCollection(pages).iterator();
	}
	
	public void setMaxPageLengthInLines(int maxPageLengthInLines) {
		this.checksum = null; //recalculate pages
		this.maxPageLengthInLines = maxPageLengthInLines;
		this.currentPageIndex = 0;
	}
	
	public void setApproxMaxLineLength(int approxMaxLineLength) {
		this.checksum = null; //recalculate pages
		this.approxMaxLineLength = approxMaxLineLength;
		this.currentPageIndex = 0;
	}

	public void removeTagInstances(Set<TagDefinition> tagDefinitions) {
		for (Page p : pages) {
			p.removeTagInstances(tagDefinitions);
		}
		
	}
	
	public int getTotalLineCount() {
		return totalLineCount;
	}
	
	public int getApproxMaxLineLength() {
		return approxMaxLineLength;
	}
	
	public int getMaxPageLengthInLines() {
		return maxPageLengthInLines;
	}
	
	public int highlight(Range absoluteHighlightRange) {
		int firstLineId = -1;
		
		for (Page page : pages) {

			Range overlappingAbsoluteRange = page.getOverlappingRange(absoluteHighlightRange);
			if (overlappingAbsoluteRange != null) {
				int lineId = page.addHighlight(overlappingAbsoluteRange);
				
				if (lineId != -1 && firstLineId == -1) {
					firstLineId = lineId;
				}
			}
		}
		
		return firstLineId;
	}

	public void removeHighlights() {
		for (Page page : pages) {
			page.removeHighlights();
		}
		
		
	}

	public List<Page> getPagesForAnnotationId(String annotationId) {
		List<Page> result = new ArrayList<>();
		for (Page page : pages) {
			if (page.contains(annotationId)) {
				result.add(page);
			}
		}
		
		return result;
	}

	public void setRightToLeftWriting(boolean rightToLeftWriting) {
		this.rightToLeftWriting = rightToLeftWriting;
	}

	public ClientComment addComment(Comment comment) {
		List<TextRange> absoluteRanges = comment.getRanges()
				.stream()
				.map(range -> new TextRange(range.getStartPoint(), range.getEndPoint()))
				.collect(Collectors.toList()); 
		List<Page> pages = getPagesForAbsoluteTextRanges(absoluteRanges);
		
		for (Page page : pages) {
			page.addAbsoluteComment(comment);
		}
		if (pages.contains(getCurrentPage())) {
			Page currentPage = getCurrentPage();
			List<TextRange> overlappingRelativRanges = new ArrayList<>();
			for (Range absoluteRange : comment.getRanges()) {
				Range overlappingAbsoluteRange = currentPage.getOverlappingRange(absoluteRange);
				if (overlappingAbsoluteRange != null) {
					TextRange overlappingRelativeRange = currentPage.getRelativeRangeFor(overlappingAbsoluteRange);
					overlappingRelativRanges.add(
						new TextRange(overlappingRelativeRange));
				}
			}
			
			
			
			return new ClientComment(
				comment.getUuid(), 
				comment.getUsername(), comment.getUserId(), 
				comment.getBody(), 
				comment.getReplyCount(),
				overlappingRelativRanges);
		}
		else {
			return null;
		}
	}
	
	public Optional<Comment> getComment(String uuid) {
		if (hasPages()) {
			Page currentPage = getCurrentPage();
			
			Optional<Comment> optionalComment = currentPage.getAbsoluteComment(uuid);
			if (optionalComment.isPresent()) {
				return optionalComment;
			}
			
		}
		
		return findComment(uuid);
	}

	private Optional<Comment> findComment(String uuid) {
		if (hasPages()) {
			for (Page page : pages) {
				Optional<Comment> optionalComment = page.getAbsoluteComment(uuid);
				if (optionalComment.isPresent()) {
					return optionalComment;
				}
			}
		}
		return Optional.empty();
	}

	public void setComments(Collection<Comment> comments) {
		comments.forEach(comment -> addComment(comment));
	}

	public void removeComment(Comment comment) {
		for (Page page : pages) {
			page.removeAbsoluteComment(comment);
		}
	}

	public void updateComments(Collection<Comment> comments) {
		for (Page page : pages) {
			page.clearComments();
		}
		setComments(comments);
	}
}
