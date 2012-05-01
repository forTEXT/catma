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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.catma.core.document.source.SourceDocument;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.result.QueryResult;
import de.catma.core.document.Range;

/**
 * A query for tagged tokens.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public class TagQuery extends Query {

    /**
     * A special comparator that has a partial equality definition that includes inclusion.
     * See {@link #compare(org.catma.indexer.TermInfo, org.catma.indexer.TermInfo)}<br>
     * <br>
     * <b>Node:</b>: this comparator imposes orderings that are inconsistent with equals!!!
     */
    private static class TagTermComparator implements Comparator<TermInfo> {
        /**
         * Compares to entries by there ranges. Two ranges are equal if the range of
         * argument o1 {@link Range#isInBetween(org.catma.document.Range) is in between} of the
         * range of argument o2.
         *
         * <b>Node:</b>: this comparator imposes orderings that are inconsistent with equals!!!
         *
         * @param o1 the first token
         * @param o2 the second token
         * @return zero for equality in the above sense,
         * the distance of the start points of the ranges else
         */
        public int compare(TermInfo o1, TermInfo o2) {

//            if(o2.getRange().isInBetween(o1.getRange())
//            		||(o1.getRange().isInBetween(o2.getRange()))) {
//                return 0;
//            }
        	if(o1.getRange().isInBetween(o2.getRange())) {
        		return 0;
        	}
            else {
                return (int)(o1.getRange().getStartPoint()-o2.getRange().getStartPoint());
            }
        }
    }


    private final static TagTermComparator TAG_TERM_COMP = new TagTermComparator();
    private String tagPhrase;
    private String tagID;

    /**
     * Constructor.
     * @param query the name of the {@link org.catma.tag.Tag}
     */
    public TagQuery(Phrase query) {
        this.tagPhrase = query.getPhrase();
    }

    public TagQuery(String tagID) {
        this.tagID = tagID;
    }

    @Override
    protected QueryResult execute() throws Exception {

        List<TermInfo> resultList = new ArrayList<TermInfo>();

//        if (tagID != null) {
//            getTermInfosForTag(TagManager.SINGLETON.getTag(tagID), resultList);
//        }
//        else {
//
//            Set<Tag> tags = TagManager.SINGLETON.getTagByName(tagPhrase);
//
//            // a Tag name does not have to be unique!
//            if (tags.size() == 1) {
//                getTermInfosForTag(tags.iterator().next(), resultList);
//            }
//            else {
//                Query curQuery = null;
//                for (Tag tag : tags) {
//                    if (curQuery == null) {
//                        curQuery = new TagQuery(tag.getID());
//                    }
//                    else {
//                        curQuery = new UnionQuery(
//                            curQuery,
//                            new TagQuery(tag.getID()));
//                    }
//                }
//
//                if (curQuery != null) {
//                    return curQuery.execute();
//                }
//            }
//        }
//
//        return new ResultList(resultList);
        return null;
    }

    /**
     * Fills the given list with the tokens that are tagged with the given Tag
     * @param tag the Tag that has to be present
     * @param resultList the list of tokens tagged with the given Tag
     */
//    private void getTermInfosForTag(Tag tag, List<TermInfo> resultList) {
//
//        SourceDocument sourceDoc = FileManager.SINGLETON.getCurrentSourceDocument();
//
//        SortedSet<Range> sortedRanges = new TreeSet<Range>();
//
//        // get all ranges that are tagged with the given Tag
//        for (StandoffMarkupDocument userMarkupDoc :
//                FileManager.SINGLETON.getUserMarkupDocumentList()) {
//
//            List<TextrangePointer> textrangePointerList =
//                    userMarkupDoc.getTextRangePointerFor(tag);
//
//            for (TextrangePointer tp : textrangePointerList) {
//
//                sortedRanges.add(tp.getRange());
//            }
//        }
//
//        //  merge the contiguous ranges
//        List<Range> mergedRanges = mergeRanges(sortedRanges);
//
//        // get the tokens for the matching ranges
//        for (Range range : mergedRanges) {
//            resultList.add(
//                new TermInfo(
//                    sourceDoc.getContent(range), range, tag));
//        }
//
//        // look for tokens that are tagged with children of the given Tag 
//        if (tag.hasTagProperty()) {
//            for( Property prop : tag.getUserDefinedProperties()) {
//                if (prop.hasTagValue()) {
//                    getTermInfosForTag((Tag)prop.getValue(), resultList);
//                }
//            }
//        }
//    }

    /**
     * Merges the contiguous ranges of the given set.
     * @param sortedRanges the ranges to merge
     * @return the merged ranges.
     */
    private List<Range> mergeRanges(SortedSet<Range> sortedRanges) {
        List<Range> result = new ArrayList<Range>();

        Range curRange = null;

        Iterator<Range> rangeIterator = sortedRanges.iterator();

        if (rangeIterator.hasNext()) {
            curRange = rangeIterator.next();

            while (rangeIterator.hasNext()) {
                Range range = rangeIterator.next();

                if (curRange.getEndPoint() == range.getStartPoint()) { // merge
                    curRange = new Range(curRange.getStartPoint(), range.getEndPoint());
                }
                else {
                    result.add(curRange);
                    curRange = range;
                }
            }
            result.add(curRange);
        }

        return result;
    }

    /**
     * @return a special comparator that defines equality via {@link Range#isInBetween(org.catma.document.Range)}.
     * @see org.catma.queryengine.TagQuery.TagTermComparator
     */
    @Override
    public Comparator<TermInfo> getComparator() {
        return TAG_TERM_COMP;
    }
}

