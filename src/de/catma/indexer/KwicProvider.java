package de.catma.indexer;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import de.catma.core.document.Range;
import de.catma.core.document.source.IndexInfoSet;
import de.catma.core.document.source.KeywordInContext;
import de.catma.core.document.source.SourceDocument;

public class KwicProvider {
	
	private String content;
	private Analyzer analyzer;
	private String sourceDocumentId;
	
	public KwicProvider(SourceDocument sourceDocument) throws IOException {
		this.sourceDocumentId = sourceDocument.getID();
		this.content = sourceDocument.getContent();
		IndexInfoSet indexInfoSet = 
				sourceDocument.getSourceContentHandler()
					.getSourceDocumentInfo().getIndexInfoSet();
		
		analyzer = 
				new WhitespaceAndPunctuationAnalyzer(
						indexInfoSet.getUnseparableCharacterSequences(),
						indexInfoSet.getUserDefinedSeparatingCharacters(),
						indexInfoSet.getLocale());
	}
	
	public List<KeywordInContext> getKwic(
			List<Range> ranges, int span) throws IOException {
		List<KeywordInContext> result = new ArrayList<KeywordInContext>();
		for (Range r : ranges) {
			result.add(getKwic(r, span));
		}
		return result;
	}
	
    public KeywordInContext getKwic(Range range, int span) throws IOException {

        SpanContext spanContext =
                getSpanContextFor(range, span, SpanDirection.Both);

        return new KeywordInContext(
            content.substring(range.getStartPoint(), range.getEndPoint()),
            content.substring(
            			spanContext.getBackwardRange().getStartPoint(),
            			spanContext.getForwardRange().getEndPoint()),
            new Range(
            		spanContext.getBackwardRange().getStartPoint(),
                    spanContext.getForwardRange().getEndPoint()),
            range.getStartPoint()-spanContext.getBackwardRange().getStartPoint());
    }	
	
    public SpanContext getSpanContextFor(Range range,
            int spanContextSize, SpanDirection direction) throws IOException{
    	
        //forward
        TokenStream forwardStream =
        	analyzer.tokenStream(
                null,
                new StringReader(
            		content.substring(
            				range.getStartPoint()))); // we start with the token included which is underlying the incoming range
        
        SpanContext spanContext = new SpanContext(sourceDocumentId);
        
        if (forwardStream.incrementToken()) { //skip first token, this is just to...
        	// ... retrieve the endoffset of the token, which might be different
        	// from the incoming range endoffset and is necessary to compute 
        	// the startOffset of the forward context
        	
        	OffsetAttribute offsetAttr =
                    (OffsetAttribute)forwardStream.getAttribute(OffsetAttribute.class);
        	
	
	        int counter = 0;
	        // the startOffset is relevant for the whole context range and content
	        // but not for the single context tokens
	        final int startOffset = Math.max(
    				range.getEndPoint(), 
    				range.getStartPoint()+offsetAttr.endOffset());
	        
	        // the endOffset gets moved forward as tokens are added to the context
	        int endOffset = startOffset;
	
	        // look for the given number of tokens in forward direction
	        while(forwardStream.incrementToken() && counter < spanContextSize) {
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
	            counter++;
	        }
	
	        spanContext.setForward(content.substring(startOffset, endOffset));
	        spanContext.setForwardRange(new Range(startOffset, endOffset));
        }
        
        if(direction.equals(SpanDirection.Both)) { //backward

            // revert content for backward search
            StringBuffer backwardBuffer =
                    new StringBuffer(
                    	content.substring(0, range.getEndPoint())); // backward stream starts at the range's end point
            
            backwardBuffer.reverse();

            TokenStream backwardStream =
                    analyzer.tokenStream(null,
                            new StringReader(backwardBuffer.toString()));
            
            if (backwardStream.incrementToken()) { // we skip the first token, its just to compute the startOffset
                OffsetAttribute offsetAttr =
                        (OffsetAttribute)backwardStream.getAttribute(OffsetAttribute.class);
                
	            int counter = 0;
	            // the startOffset gets moved backward as we add tokens to the context
	            int startOffset = 
	            		Math.min(
	            				range.getStartPoint(), 
	            				range.getEndPoint()-offsetAttr.endOffset());
	            
	            final int endOffset = startOffset;
	            
	            // look for the given number of tokens in backward direction
	            while(backwardStream.incrementToken() && counter < spanContextSize) {
	                offsetAttr =
	                        (OffsetAttribute)backwardStream.getAttribute(OffsetAttribute.class);
	
	                CharTermAttribute termAttr =
	                        (CharTermAttribute)backwardStream.getAttribute(CharTermAttribute.class);
	                // move startoffset backward
	                startOffset = range.getEndPoint()-offsetAttr.endOffset();
	                // start and endoffset are computed with the range's endpoint
	                // because that's where we started the backward stream
	                TermInfo ti =  new TermInfo( 
	                	new StringBuilder(termAttr.toString()).reverse().toString(),
	                    	startOffset, range.getEndPoint()-offsetAttr.startOffset());
	
	                spanContext.addBackwardToken(ti);
	                counter++;
	            }
	            
	            spanContext.setBackward(content.substring(startOffset, endOffset));
	            spanContext.setBackwardRange(new Range(startOffset, endOffset));
            }
        }

        return spanContext;
    }
}
