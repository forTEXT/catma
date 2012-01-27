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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import de.catma.ui.client.ui.tagger.shared.TagInstance;

/**
 * @author marco.petris@web.de
 *
 */
public class Pager implements Iterable<Page> {
	
	public static interface PagerListener {
		public void textChanged();
	}
	
	static final String LINE_CONTENT_PATTERN = 
			"(\\S+)|(\\p{Blank}+)|(\r\n|[\n\r\u2028\u2029\u0085])";
	
	static int WORDCHARACTER_GROUP = 1;
	static int WHITESPACE_GROUP = 2;
	static int LINE_SEPARATOR_GROUP = 3;


	private ArrayList<Page> pages;
	private int currentPageIndex=0;

	private int approxMaxLineLength;
	private int maxPageLengthInLines;
	private PagerListener pagerListener;
	private Long checksum = null;
	
	public Pager(int approxMaxLineLength, int maxPageLengthInLines) {
		pages = new ArrayList<Page>();
		this.approxMaxLineLength = approxMaxLineLength;
		this.maxPageLengthInLines = maxPageLengthInLines;
	}
	
	public void setText(String text) {
		if (!matchChecksum(text)) {
			pages.clear();
			buildPages(text);
			if (pagerListener != null) {
				pagerListener.textChanged();
			}
		}
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
		Matcher matcher = Pattern.compile(LINE_CONTENT_PATTERN).matcher(text);

		int pageStart = 0;
		int pageEnd = 0;
		int pageLines = 0;
		
		int lineLength = 0;

		while(matcher.find()) {
			if (lineLength + matcher.group().length()>approxMaxLineLength) {
				pageLines++;
				pageEnd+=lineLength;
				lineLength = 0;
			}			
			
			if (pageLines >= maxPageLengthInLines) {
				pages.add(new Page(text.substring(pageStart, pageEnd), pageStart, pageEnd));
				pageLines = 0;
				pageStart = pageEnd;
			}

			lineLength += matcher.group().length();
			
			if (matcher.group(LINE_SEPARATOR_GROUP) != null) {
				pageLines++;
				pageEnd+=lineLength;
				lineLength = 0;
			}
		}
		
		if (lineLength != 0) {
			pageEnd+=lineLength;
			pageLines++;
		}
		
		if (pageLines != 0) {
			pages.add(new Page(text.substring(pageStart, pageEnd), pageStart, pageEnd));
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

	public Page getPageFor(TagInstance ti) {
		
		for (Page p : pages) {
			if (p.includes(ti)) {
				return p;
			}
		}
		
		return null;
	}
	
	public Iterator<Page> iterator() {
		return Collections.unmodifiableCollection(pages).iterator();
	}
}
