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

package de.catma.queryengine;

import java.util.List;

import de.catma.indexer.TermInfo;
import de.catma.queryengine.result.QueryResult;

/**
 * A collocation query looks for terms that form a collocation with other terms within a given
 * span context. 
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class CollocQuery extends Query {

    public static final int DEFAULT_SPANCONTEXT_SIZE = 5;
    
    private Query query1;
    private Query query2;
    private int spanContextSize;
    private SpanDirection direction;

    /**
     * Constructor.
     * @param query1 the definiton of the search term
     * @param query2 the defintion of the collocation term
     * @param spanContext the size of the context
     * (needs to be parseable to {@link Integer#parseInt(String) Integer}) 
     * or <code>null</code> for the default size.
     * @param direction the direction of the span context
     */
    public CollocQuery(Query query1, Query query2, String spanContext, SpanDirection direction) {
        this.query1 = query1;
        this.query2 = query2;
        if (spanContext == null) {
            spanContextSize = DEFAULT_SPANCONTEXT_SIZE;
        }
        else {
            spanContextSize = Integer.parseInt(spanContext);
        }
        this.direction = direction;
    }

    /**
     * Constructor. ({@link org.catma.queryengine.SpanDirection#Both both} directions are used as the span context)
     * @param query1 the definiton of the search term
     * @param query2 the defintion of the collocation term
     * @param spanContext the size of the context
     * (needs to be parseable to {@link Integer#parseInt(String) Integer})
     * or <code>null</code> for the default size.
     */
    public CollocQuery(Query query1, Query query2, String spanContext) {
        this(query1, query2, spanContext, SpanDirection.Both);
    }

    @Override
    protected QueryResult execute() throws Exception {
//    	QueryResult qr = 

//        SourceDocument sourceDoc = FileManager.SINGLETON.getCurrentSourceDocument();
//        Index index = sourceDoc.getIndex();
//
//        ResultList result1 = query1.execute();
//        List<TermInfo> termInfoList1 = result1.getTermInfoList();
//
//        ResultList result2 = query2.execute();

        // loop over the first resultset get the collocations and
        // check the collocation condition
//        Iterator<TermInfo> iter = termInfoList1.iterator();
//        while (iter.hasNext()) {
//            TermInfo curTermInfo = iter.next();
//
//            SpanContext spanContext = index.getSpanContextFor(
//                    sourceDoc.getContent(), curTermInfo.getRange(),
//                    spanContextSize, direction);
//
//            if ((!spanContextMeetsCollocCondition(
//                    spanContext.getForwardTokens(), result2, index))
//                   && ( ( !spanContext.hasBackwardSpan() )
//                        || ((!spanContextMeetsCollocCondition(
//                                spanContext.getBackwardTokens(), result2, index))))) {
//                iter.remove(); // this one does not meet the condition
//            }
//        }
//
//        return new ResultList(termInfoList1);
    	return null;
    }

    /**
     * Checks if the given span context tokens meets the tokens of the given condition.
     * 
     * @param spanContext the tokens of the span context
     * @param collocConditionList the list of conditions (tokens to be present in the span context)
     * @param index the index of the {@link org.catma.document.source.SourceDocument}
     * @return true if the given span context meets the collocation condition
     * @throws IOException {@link org.catma.indexer.Index}-access problems, see instance for details 
     */
//    private boolean spanContextMeetsCollocCondition(
//            List<TermInfo> spanContext , ResultList collocConditionList, Index index) throws IOException {
//
//        for (TermInfo collocCondition : collocConditionList.getTermInfoList()) {
//            if(matches(spanContext, index.extractTermInfoFrom(
//                    collocCondition.getTerm(),
//                    (int)collocCondition.getRange().getStartPoint()))) {
//                return true;
//            }
//        }
//
//        return false;
//    }

    /**
     * @param spanContextTokens the tokens of the span context
     * @param collocConditionTerms the tokens of the collocation condition
     * @return true if all the tokens of the collocation condition can be found in that order in the
     * list of the span context tokens
     */
    private boolean matches(List<TermInfo> spanContextTokens,
                            List<TermInfo> collocConditionTerms) {

        // span context should be large enough to hold the tokens of the condition
        if (spanContextTokens.size() < collocConditionTerms.size()) {
            // no need for detailed checking then
            return false;
        }

        if (spanContextTokens.size() == 0) {
            // at this point we realize that we have no tokens as a condition (see previous check)
            // and no tokens in the context as well, that matches!
            return true;
        }

        // we are trying to find the first token of the condition in the span context
        int startIdx = getFirstMatchingIndex(spanContextTokens, collocConditionTerms.get(0));

        if (startIdx==-1) { // not even the first token is present
            return false;
        }

        // first token was present at this time

        // are there more tokens with this condition?
        if (collocConditionTerms.size() == 1) {
            // no, ok we are done
            return true;
        }

        // are there enough tokens in the span context to possibly meet the condition tokens?
        if ((collocConditionTerms.size() + startIdx) > spanContextTokens.size()) {
            // no need for detailed comparing then
            return false;
        }

        // ok, we have found the first token and there are enough tokens in the span context
        // after the one we found already, so lets try to check the rest of the tokens
        // of the condition
        for (int idx = 1; idx <collocConditionTerms.size(); idx++) {
            if (!spanContextTokens.get(startIdx+idx).equals(collocConditionTerms.get(idx))) {
                // the condition is not satisified with this one
                return false;
            }
        }

        // ok, found all tokens of the condition in the right order
        return true;
    }

    /**
     * Retrieves the first matching index within the list of span context tokens.<br>
     * <br>
     * Note: not only the type of the token but also its range is checked for equality!
     *
     * @param spanContextTokens the tokens of the span context
     * @param collocConditionTerm the current token of the collocation condition
     * @return the first matching index within the list of span context tokens or -1 for no match
     */
    private int getFirstMatchingIndex(List<TermInfo> spanContextTokens, TermInfo collocConditionTerm) {

        for( int idx = 0; idx < spanContextTokens.size(); idx++) {
            if(spanContextTokens.get(idx).equals(collocConditionTerm)) {
                return idx;
            }
        }

        return -1;
    }
}

