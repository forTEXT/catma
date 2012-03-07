/*
 *    CATMA Computer Aided Text Markup and Analysis
 * 
 *    Copyright (C) 2009  University Of Hamburg
 * 
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 * 
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.catma.indexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import de.catma.Log;
import de.catma.LogText;
import de.catma.core.document.Range;
import de.catma.queryengine.CompareOperator;
import de.catma.queryengine.Simil;
import de.catma.queryengine.SpanDirection;
import de.catma.queryengine.result.LazyQueryResultRow;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRowList;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An index that holds the indexed content of a {@link org.catma.document.source.SourceDocument}.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class Index {
    private Directory directory;
    private Analyzer analyzer;
    private int totalTokenCount;

    /**
     * Constructor
     * @param dir the directory where the index is stored
     * @param analyzer the analyzer used to build the index
     * @param totalTokenCount the total token count (word-instance count)
     */
    public Index(Directory dir, Analyzer analyzer, int totalTokenCount) {
        this.directory = dir;
        this.analyzer = analyzer;
        this.totalTokenCount = totalTokenCount;
    }

    /**
     * Extracts a list of terms from the given string, the offset of the resulting terms are
     * computed in respect to the given baseOffset.
     * 
     * @param phrase the phrase to extract from
     * @param baseOffset the base offset that will be added to the computed offsets
     * @return a list of terms extracted from the phrase
     * @throws IOException see {@link org.apache.lucene.analysis.TokenStream#incrementToken()}
     */
    public List<TermInfo> extractTermInfoFrom(String phrase, int baseOffset) throws IOException {
        TokenStream tokenStream =
                analyzer.tokenStream(
                        null, // our analyzer does not use the fieldname 
                        new StringReader(phrase));

        List<TermInfo> termInfoList = new ArrayList<TermInfo>();

        while(tokenStream.incrementToken()) {
            TermAttribute termAttr =
                    (TermAttribute)tokenStream.getAttribute(TermAttribute.class);
            OffsetAttribute offsetAttr =
                    (OffsetAttribute)tokenStream.getAttribute(OffsetAttribute.class);

            TermInfo ti =  new TermInfo(termAttr.term(),
                baseOffset+offsetAttr.startOffset(), baseOffset+offsetAttr.endOffset());
            termInfoList.add(ti);
        }

        return termInfoList;
    }

    /**
     * Searches the index for the given term (type).
     * @param term the term to search for
     * @return a list of tokens (type occurrences) 
     * @throws IOException see {@link org.apache.lucene.search.IndexSearcher#IndexSearcher(org.apache.lucene.store.Directory, boolean)}
     */
    public List<TermInfo> search(String term) throws IOException {
                                                                    
        List<TermInfo> searchHits = Collections.emptyList();

        IndexSearcher isearcher = null;
        try {
            isearcher = new IndexSearcher(directory, true);
            Log.DEFAULT_LOGGER.fine(LogText.SINGLETON.getString("Index.searchingForTerm",term));
            TermQuery termQuery = new TermQuery(new Term(Fieldname.term.name(), term));

            TopDocs hits = isearcher.search(termQuery,isearcher.maxDoc());

            searchHits = new ArrayList<TermInfo>();

            for (ScoreDoc sd : hits.scoreDocs) {
                Document hitDoc = isearcher.doc(sd.doc);
                Log.DEFAULT_LOGGER.fine(
                        LogText.SINGLETON.getString(
                                "Index.foundTerm",
                                hitDoc.get(Fieldname.term.name()),
                                hitDoc.get(Fieldname.startOffset.name()),
                                hitDoc.get(Fieldname.endOffset.name())));

                searchHits.add(
                    new TermInfo(
                        hitDoc.get(Fieldname.term.name()),
                        hitDoc.get(Fieldname.startOffset.name()),
                        hitDoc.get(Fieldname.endOffset.name())));

            }
        }
        finally {
            if(isearcher != null) {
                isearcher.close();
            }
        }

        return searchHits;
    }

    /**
     * Create a span context.
     *
     * @param content the content to search in
     * @param range the range of the keyword
     * @param spanContextSize the size of the context/ per direction
     * @param direction the direction of the context
     * @return the created span context
     * @throws IOException see {@link org.apache.lucene.analysis.TokenStream#incrementToken()}
     */
    public SpanContext getSpanContextFor(
            String content, Range range,
            int spanContextSize, SpanDirection direction) throws IOException{

        //forward
        TokenStream forwardStream =
                analyzer.tokenStream(
                        null,
                        new StringReader(
                                content.substring((int)range.getEndPoint())));

        SpanContext spanContext = new SpanContext();

        int counter = 0;
        int startOffset = (int)range.getEndPoint();
        int endOffset = (int)range.getEndPoint();

        // look for the given number of tokens in forward direction
        while(forwardStream.incrementToken() && counter < spanContextSize) {
            OffsetAttribute offsetAttr =
                    (OffsetAttribute)forwardStream.getAttribute(OffsetAttribute.class);

            TermAttribute termAttr =
                    (TermAttribute)forwardStream.getAttribute(TermAttribute.class);

            endOffset = (int)range.getEndPoint()+offsetAttr.endOffset();
            TermInfo ti =  new TermInfo(termAttr.term(),
                (int)range.getEndPoint()+offsetAttr.startOffset(),
                (int)range.getEndPoint()+offsetAttr.endOffset());
            spanContext.addForwardToken(ti);
            counter++;
        }

        spanContext.setForward(content.substring(startOffset, endOffset));
        spanContext.setForwardRange(new Range(startOffset, endOffset));

        if(direction.equals(SpanDirection.Both)) { //backward

            // revert content for backward search
            StringBuffer backwardBuffer =
                    new StringBuffer(content.substring(0,(int)range.getStartPoint()));

            backwardBuffer.reverse();

            TokenStream backwardStream =
                    analyzer.tokenStream(null,
                            new StringReader(backwardBuffer.toString()));
            
            counter = 0;
            startOffset = (int)range.getStartPoint();
            endOffset = (int)range.getStartPoint();
            
            // look for the given number of tokens in backward direction
            while(backwardStream.incrementToken() && counter < spanContextSize) {
                OffsetAttribute offsetAttr =
                        (OffsetAttribute)backwardStream.getAttribute(OffsetAttribute.class);

                TermAttribute termAttr =
                        (TermAttribute)backwardStream.getAttribute(TermAttribute.class);

                startOffset = (int)range.getStartPoint()-offsetAttr.endOffset();
                TermInfo ti =  new TermInfo( new StringBuffer(termAttr.term()).reverse().toString(),
                    startOffset, (int)range.getStartPoint()-offsetAttr.startOffset());

                spanContext.addBackwardToken(ti);
                counter++;
            }
            
            spanContext.setBackward(content.substring(startOffset, endOffset));
            spanContext.setBackwardRange(new Range(startOffset, endOffset));
        }

        return spanContext;
    }

    /**
     * Search terms that match the given frequency condition
     * @param operator1 the operator to compare the type frequency with the given frequency
     * @param freq1 the frequency to compare with
     * @return the list of terms that match the frequency condition
     * @throws IOException see {@link org.apache.lucene.search.IndexSearcher#IndexSearcher(org.apache.lucene.store.Directory, boolean)}
     */
    public List<TermInfo> searchTermsByFrequency(
            CompareOperator operator1,  int freq1) throws IOException {
        return searchTermsByFrequency(operator1, freq1, null, 0);
    }

    /**
     * Search terms that match the given frequency condition
     *
     * @param operator1 the operator to compare the type frequency with the given frequency
     * @param freq1 the frequency to compare with
     * @param operator2 a second operator to compare with, can be <code>null</code>
     * @param freq2 a second frequency to compare with, use 0 if operator2 is set to <code>null</code> 
     * @return the list of terms that match the frequency condition(s)
     * @throws IOException see {@link org.apache.lucene.search.IndexSearcher#IndexSearcher(org.apache.lucene.store.Directory, boolean)}
     */
    public List<TermInfo> searchTermsByFrequency(
            CompareOperator operator1,  int freq1, CompareOperator operator2,  int freq2) throws IOException {
        
        IndexSearcher isearcher = null;

        List<TermInfo> searchHits = new ArrayList<TermInfo>();

        try {
            isearcher = new IndexSearcher(directory, true);
            TermEnum terms = isearcher.getIndexReader().terms();
            // loop through the terms and test the conditions
            while (terms.next()) {
                if (operator1.getCondition().isTrue( terms.docFreq(), freq1 )
                        && ( (operator2 == null)
                            || (operator2.getCondition().isTrue(terms.docFreq(), freq2)) )) {
                    List<TermInfo> curResult = search(terms.term().text());
                    searchHits.addAll(curResult);
                }
            }
        }
        finally {
            if(isearcher != null) {
                isearcher.close();
            }
        }

        return searchHits;
    }

    /**
     * Searches a list of terms that a similar to the given phrase by a certain degree
     *
     * @param phrase the phrase to compare with
     * @param similPercent the degree of similarity
     * @return the list of similar terms
     * @throws IOException see {@link org.apache.lucene.search.IndexSearcher#IndexSearcher(org.apache.lucene.store.Directory, boolean)}
     * @see org.catma.queryengine.Simil
     */
    public List<TermInfo> searchTermsBySimilarity(String phrase, int similPercent) throws IOException {
        List<TermInfo> searchHits = new ArrayList<TermInfo>();

        Simil simil = new Simil(phrase);
        IndexSearcher isearcher = null;
        TermEnum terms = null;

        try {
            isearcher = new IndexSearcher(directory, true);
            terms = isearcher.getIndexReader().terms();
            // loop through the terms and test the condition
            while(terms.next()) {
                if(simil.getSimilarityInPercentFor(terms.term().text()) >= similPercent) {
                    List<TermInfo> curResult = search(terms.term().text());
                    searchHits.addAll(curResult);
                }
            }
        }
        finally {
            if (terms != null) {
                terms.close();
            }
            
            if(isearcher != null) {
                isearcher.close();
            }
        }

        return searchHits;
    }

    /**
     * @return the list of types with not yet initialized token lists
     * @throws IOException see {@link org.apache.lucene.search.IndexSearcher#IndexSearcher(org.apache.lucene.store.Directory, boolean)}
     * @see org.catma.queryengine.result.LazyQueryResultRow
     */
    public QueryResult getWordlist() throws IOException {
        QueryResultRowList result = new QueryResultRowList();

        IndexSearcher isearcher = null;
        TermEnum terms = null;

        try {
            isearcher = new IndexSearcher(directory, true);
            terms = isearcher.getIndexReader().terms();
            while(terms.next()) {
                result.add(new LazyQueryResultRow(terms.term().text(),terms.docFreq()));
            }
        }
        finally {
            if (terms != null) {
                terms.close();
            }

            if(isearcher != null) {
                isearcher.close();
            }
        }
        return result;
    }

    /**
     * @return the total token count (word-instance count)
     */
    public int getTotalTokenCount() {
        return totalTokenCount;
    }
}
