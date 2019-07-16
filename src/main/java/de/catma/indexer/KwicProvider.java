/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
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
package de.catma.indexer;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import de.catma.document.Range;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocument;

public class KwicProvider {
	
	private String content;
	private String sourceDocumentId;
	private String sourceDocumentName;
	private SourceDocument sourceDocument;
	private IndexInfoSet indexInfoSet;
	
	public KwicProvider(SourceDocument sourceDocument) throws IOException {
		this.sourceDocumentId = sourceDocument.getID();
		this.content = sourceDocument.getContent();
		this.sourceDocumentName = sourceDocument.toString();
		this.sourceDocument= sourceDocument;
		indexInfoSet = 
				sourceDocument.getSourceContentHandler()
					.getSourceDocumentInfo().getIndexInfoSet();
	}
	
	public List<KeywordInSpanContext> getKwic(
			List<Range> ranges, int span) throws IOException {
		List<KeywordInSpanContext> result = new ArrayList<KeywordInSpanContext>();
		for (Range r : ranges) {
			result.add(getKwic(r, span));
		}
		return result;
	}
	
    public KeywordInSpanContext getKwic(Range range, int span) throws IOException {

        SpanContext spanContext =
                getSpanContextFor(range, span, SpanDirection.BOTH);

        return new KeywordInSpanContext(
            content.substring(range.getStartPoint(), range.getEndPoint()),
            content.substring(
            			spanContext.getBackwardRange().getStartPoint(),
            			spanContext.getForwardRange().getEndPoint()),
            new Range(
            		spanContext.getBackwardRange().getStartPoint(),
                    spanContext.getForwardRange().getEndPoint()),
            range.getStartPoint()-spanContext.getBackwardRange().getStartPoint(),
            indexInfoSet.isRightToLeftWriting(),
            spanContext);
    }	
	
    public SpanContext getSpanContextFor(Range range,
            int spanContextSize, SpanDirection direction) throws IOException{
    	
    	try (WhitespaceAndPunctuationAnalyzer forwardAnalyzer = 
				new WhitespaceAndPunctuationAnalyzer(
						indexInfoSet.getUnseparableCharacterSequences(),
						indexInfoSet.getUserDefinedSeparatingCharacters(),
						indexInfoSet.getLocale())) {
    		
	        //forward
	        PeekableTokenStream forwardStream =
	        	new PeekableTokenStream(
		        	forwardAnalyzer.tokenStream(
		                null,
		                new StringReader(
		            		content.substring(
		            				range.getStartPoint())))); // we start with all the ...
	        // ...tokens included which are underlying the incoming range
	        // that is because we have to examine the tokens at the edges of that range
	        // if there happens to be a partial token we do not want 
	        // to include the other part into the context because the SpanContext includes 
	        // only full tokens
	        
	        SpanContext spanContext = new SpanContext(sourceDocumentId);
	    
	        if (moveForwardStreamToLastTokenOfRange(forwardStream, range)) { //move stream ...
	        	// ... to the token at the right edge of the range 
	        	// if it is a partial token we want to exclude the part outside 
	        	// of the range from the right side context by setting the
	        	// startOffset accordingly (see below)
	        	
	        	OffsetAttribute offsetAttr =
	                    (OffsetAttribute)forwardStream.getAttribute(OffsetAttribute.class);
	        	
		        int contextTokenCounter = 0;
		        
		        // if the range is exactly at a token boundary 
		        // the range.getEndPoint() equals range.getStartPoint()+offsetAttr.endOffset()
		        // else range.getEndPoint() is greater than range.getStartPoint()+offsetAttr.endOffset()
		        // because the first includes a parital token and the latter includes the full token
		        // we set the startOffset to include only full tokens
		        final int startOffset = Math.max(
	    				range.getEndPoint(), 
	    				range.getStartPoint()+offsetAttr.endOffset());
		        
		        // the endOffset gets moved forward as tokens are added to the context
		        int endOffset = startOffset;
		
		        // look for the given number of tokens in forward direction
		        while(forwardStream.incrementToken() && contextTokenCounter < spanContextSize) {
		            offsetAttr =
		                    (OffsetAttribute)forwardStream.getAttribute(OffsetAttribute.class);
		
		            CharTermAttribute termAttr = 
		            	(CharTermAttribute)forwardStream.getAttribute(CharTermAttribute.class);
		            // move the endOffset forward
		            endOffset = range.getStartPoint()+offsetAttr.endOffset();
		            
		            TermInfo ti =  
		            		new TermInfo(
		            				termAttr.toString(),
		            				// we compute offsets with the range's startpoint, 
		            				// because that's where we started the tokenstream
		            				range.getStartPoint()+offsetAttr.startOffset(),
		            				range.getStartPoint()+offsetAttr.endOffset());
		            spanContext.addForwardToken(ti);
		            contextTokenCounter++;
		        }
		
		        spanContext.setForward(content.substring(startOffset, endOffset));
		        spanContext.setForwardRange(new Range(startOffset, endOffset));
	        }
	        else {
	        	spanContext.setForward("");
	 	        spanContext.setForwardRange(new Range(content.length(), content.length()));
	        }
	        
	        if(direction.equals(SpanDirection.BOTH)) { //backward
	
	            // revert content for backward search
	            StringBuffer backwardBuffer =
	                    new StringBuffer(
	                    	content.substring(0, range.getEndPoint())); // backward stream starts at the range's end point
	            
	            backwardBuffer.reverse();
	            try (WhitespaceAndPunctuationAnalyzer backwardAnalyzer = 
    				new WhitespaceAndPunctuationAnalyzer(
    						indexInfoSet.getUnseparableCharacterSequences(),
    						indexInfoSet.getUserDefinedSeparatingCharacters(),
    						indexInfoSet.getLocale())) {
		            PeekableTokenStream backwardStream =
		                    new PeekableTokenStream(
		                    	backwardAnalyzer.tokenStream(null,
		                            new StringReader(backwardBuffer.toString())));
		            
		            if (moveBackwardStreamToFirstTokenOfRange(backwardStream, range)) {  //move stream ...
		            	// ... to the token at the left edge of the range 
		            	// if it is a partial token we want to exclude the part outside 
		            	// of the range from the left side context by setting the
		            	// endoffset accordingly (see below)
		            	
		                OffsetAttribute offsetAttr =
		                        (OffsetAttribute)backwardStream.getAttribute(OffsetAttribute.class);
		                
			            int contextTokenCounter = 0;
			            
			            // if the range is exactly at a token boundary 
				        // the range.getStartPoint() equals range.getEndPoint()-offsetAttr.endOffset()
				        // else range.getEndPoint()-offsetAttr.endOffset() is smaller than
			            // range.getStartPoint() because the first includes full tokens
			            // and the latter includes a partial token
				        // we set the startOffset and therefore endOffset to include full tokens only
			            
			            // the startOffset gets moved backward as we add tokens to the context
			            int startOffset = 
			            		Math.min(
			            				range.getStartPoint(), 
			            				range.getEndPoint()-offsetAttr.endOffset());
			            
			            // the endoffset stays fix 
			            final int endOffset = startOffset;
			            
			            // look for the given number of tokens in backward direction
			            while(backwardStream.incrementToken() && contextTokenCounter < spanContextSize) {
			                offsetAttr =
			                        (OffsetAttribute)backwardStream.getAttribute(
			                        		OffsetAttribute.class);
			
			                CharTermAttribute termAttr =
			                        (CharTermAttribute)backwardStream.getAttribute(
			                        		CharTermAttribute.class);
			                // move startoffset backward
			                startOffset = range.getEndPoint()-offsetAttr.endOffset();
			                // start and endoffset are computed with the range's endpoint
			                // because that's where we started the backward stream
			                TermInfo ti =  new TermInfo( 
			                	new StringBuilder(termAttr.toString()).reverse().toString(),
			                    	startOffset, range.getEndPoint()-offsetAttr.startOffset());
			
			                spanContext.addBackwardToken(ti);
			                contextTokenCounter++;
			            }
			            
			            spanContext.setBackward(content.substring(startOffset, endOffset));
			            spanContext.setBackwardRange(new Range(startOffset, endOffset));
		            }
		            else {
		            	spanContext.setBackward("");
		            	spanContext.setBackwardRange(new Range(0,0));
		            }
	            }
	        }
	        return spanContext;
    	}
    }

	private boolean moveForwardStreamToLastTokenOfRange(
			PeekableTokenStream forwardStream,
			Range range) throws IOException {
		
		while (forwardStream.incrementToken()) {
			
			if (forwardStream.canPeek()) {
				OffsetAttribute offsetAttr = 
						forwardStream.peekAttribute(OffsetAttribute.class);
				if (range.getStartPoint()+offsetAttr.endOffset() > range.getEndPoint()) {
					return true;
				}
			}
			
		}
		
		return false;
	}
	
	private boolean moveBackwardStreamToFirstTokenOfRange(
			PeekableTokenStream forwardStream,
			Range range) throws IOException {
		
		while (forwardStream.incrementToken()) {
			
			if (forwardStream.canPeek()) {
				OffsetAttribute offsetAttr = 
						forwardStream.peekAttribute(OffsetAttribute.class);
				if (range.getEndPoint()-offsetAttr.endOffset() < range.getStartPoint()) {
					return true;
				}
			}
			
		}
		
		return false;
	}
	
	public int getDocumentLength() {
		return content.length();
	}
	
	public String getContent() {
		return content;
	}
	
	public String getSourceDocumentName() {
		return sourceDocumentName;
	}

	public SourceDocument getSourceDocument() {
		return sourceDocument;
	}

	
	
}
