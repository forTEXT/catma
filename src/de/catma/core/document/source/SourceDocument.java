/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009  University Of Hamburg
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

package de.catma.core.document.source;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.catma.core.document.Range;

/**
 * The representation of a Source Document.
 *
 * @author Marco Petris
 *
 */
public class SourceDocument {
	
	private SourceContentHandler handler;
	private long checksum;
	private URI uri;
	
	/**
	 * Constructor.
	 * 
	 * @param sourceDocumentInfo the corresponding Structure Markup Document
	 * @param handler the handler for the Source Document
	 * @param name the name of the document
	 * @param progressListener a listener which will be notified of the progress
	 * of the computation of the checksum.
	 * @throws IOException access failure
	 * @see SourceContentHandler
	 * @see MetaDataInfoSet
	 */
	SourceDocument( 
			SourceDocumentInfo sourceDocumentInfo,
			SourceContentHandler handler, 
			long checksum) throws IOException {
		this.uri = sourceDocumentInfo.getURI();
		this.handler = handler;
		this.checksum = checksum;
	}
	
	/**
	 * Displays the content of the document as text.
	 */
	@Override
	public String toString() {
		return getContent( 0 ).toString();
	}

	/**
	 * @param range the range of the content
	 * @return the content between the startpoint and the endpoint of the range
	 */
	public String getContent( Range range ) {
		return getContent( range.getStartPoint(), range.getEndPoint() );
	}
	
	/**
	 * @param startPoint startpoint of the content
	 * @param endPoint endpoint of the content
	 * @return the content between the startpoint and the endpoint
	 */
	public String getContent( long startPoint, long endPoint ) {
		return handler.getContent( startPoint, endPoint );
	}
	
	/**
	 * @param startPoint startpoint of the content
	 * @return the content after the startpoint
	 */
	public String getContent( long startPoint ) {
		return handler.getContent( startPoint );
	}
	
	/**
	 * @return the size of the document
	 */
	public long getSize() {
		return handler.getContent( 0 ).length();
	}
	
	/**
	 * @param patternString the pattern to look for
	 * @param isRegularExpression true->pattern is a regular 
	 * expression-{@link Pattern}, false->simple match
	 * @return a list of ranges where this document has content with 
	 * the given pattern
	 */
	public List<Range> getRangesFor( 
			String patternString, boolean isRegularExpression ) {
		
		ArrayList<Range> result = new ArrayList<Range>();
		
		String content = getContent(0);
		
		Pattern pattern = null; 
			
		if( !isRegularExpression ) {
			pattern = Pattern.compile( 
				Pattern.quote( patternString ),
				Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE );
		}
		else {
			pattern = Pattern.compile( patternString ); 
		}

		Matcher matcher = pattern.matcher( content );
		
		while( matcher.find() ) {
			MatchResult mr = matcher.toMatchResult();
			
//			System.out.println( 
//				"start: " + mr.start() 
//				+ " end: " + mr.end() 
//				+ " token: " + mr.group() ); 
		
			result.add( 
					new Range(
						mr.start(), 
						mr.end() ) );
		}
		
		return result;
	}

	/**
	 * @return the checksum of the Source Document
	 */
	public long getChecksum() {
		return checksum;
	}
	
	public URI getURI() {
		return uri;
	}
}
