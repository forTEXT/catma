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
    	
    	//TODO: look for correct token range
    	
        //forward
        TokenStream forwardStream =
        	analyzer.tokenStream(
                null,
                new StringReader(
            		content.substring(
            				range.getEndPoint())));

        SpanContext spanContext = new SpanContext(sourceDocumentId);

        int counter = 0;
        int startOffset = range.getEndPoint();
        int endOffset = range.getEndPoint();

        // look for the given number of tokens in forward direction
        while(forwardStream.incrementToken() && counter < spanContextSize) {
            OffsetAttribute offsetAttr =
                    (OffsetAttribute)forwardStream.getAttribute(OffsetAttribute.class);

            CharTermAttribute termAttr = 
            	(CharTermAttribute)forwardStream.getAttribute(CharTermAttribute.class);

            endOffset = range.getEndPoint()+offsetAttr.endOffset();
            TermInfo ti =  
            		new TermInfo(
            				termAttr.toString(),
            				range.getEndPoint()+offsetAttr.startOffset(),
            				range.getEndPoint()+offsetAttr.endOffset());
            spanContext.addForwardToken(ti);
            counter++;
        }

        spanContext.setForward(content.substring(startOffset, endOffset));
        spanContext.setForwardRange(new Range(startOffset, endOffset));

        if(direction.equals(SpanDirection.Both)) { //backward

            // revert content for backward search
            StringBuffer backwardBuffer =
                    new StringBuffer(
                    	content.substring(0, range.getStartPoint()));
            
            backwardBuffer.reverse();

            TokenStream backwardStream =
                    analyzer.tokenStream(null,
                            new StringReader(backwardBuffer.toString()));
            
            counter = 0;
            startOffset = range.getStartPoint();
            endOffset = range.getStartPoint();
            
            // look for the given number of tokens in backward direction
            while(backwardStream.incrementToken() && counter < spanContextSize) {
                OffsetAttribute offsetAttr =
                        (OffsetAttribute)backwardStream.getAttribute(OffsetAttribute.class);

                CharTermAttribute termAttr =
                        (CharTermAttribute)backwardStream.getAttribute(CharTermAttribute.class);

                startOffset = range.getStartPoint()-offsetAttr.endOffset();
                TermInfo ti =  new TermInfo( 
                	new StringBuilder(termAttr.toString()).reverse().toString(),
                    	startOffset, range.getStartPoint()-offsetAttr.startOffset());

                spanContext.addBackwardToken(ti);
                counter++;
            }
            
            spanContext.setBackward(content.substring(startOffset, endOffset));
            spanContext.setBackwardRange(new Range(startOffset, endOffset));
        }

        return spanContext;
    }
}
