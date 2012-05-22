/*
 * CATMA Computer Aided Text Markup and Analysis
 *
 *    Copyright (C) 2008-2010  University Of Hamburg
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

import java.util.Set;

import de.catma.queryengine.result.QueryResult;


/**
 * This query looks for tokens that are tagged with a {@link Tag} that has the desired
 * {@link org.DBProperty.tag.Property}.
 *
 * @author Marco Petris
 *
 */
public class PropertyQuery extends Query {

    private String propertyName;
    private String propertyValue;

    /**
     * Constructor
     * @param property the name of the {@link org.DBProperty.tag.Property}
     * @param value the value of the {@link org.DBProperty.tag.Property} this is optional and can be
     * <code>null</code>
     */
    public PropertyQuery(Phrase property, Phrase value) {
        propertyName = property.getPhrase();
        if (value != null) {
            propertyValue = value.getPhrase();
        }
        else {
            propertyValue = null;
        }
    }

    @Override
    protected QueryResult execute() throws Exception {

//        Set<Tagset> tagsets = TagManager.SINGLETON.getTagsets();
//
//        Query curQuery = null;
//
//        // loop over the tagsets
//        for (Tagset tagset : tagsets) {
//            // loop over the tags
//            for (Tag tag : tagset) {
//                Property p = tag.getProperty(propertyName);
//
//                // does this property meet the query condition?
//                if ((p != null)
//                        && ((propertyValue == null) || p.getValue().equals(propertyValue))) {
//
//                    // is this the first Tag that matches?
//                    if (curQuery == null) {
//                        // yes, ok we create a tag query for this Tag
//                        curQuery = new TagQuery(new Phrase("\""+tag.getName()+"\""));
//                    }
//                    else {
//                        // no, so we create a tag query for this Tag and combine
//                        // it with the previous matches via a union query
//                        curQuery = new UnionQuery(
//                            curQuery,
//                            new TagQuery(new Phrase("\""+tag.getName()+"\"")));
//                    }
//                    
//                }
//            }
//        }
//
//        if (curQuery != null) {
//            return curQuery.execute();
//        }
//        else {
//            return new ResultList();
//        }
    	return null;
    }
}
