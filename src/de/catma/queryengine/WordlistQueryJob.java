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

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.queryengine.result.QueryResult;
import de.catma.queryengine.result.QueryResultRow;

/**
 * This job initializes {@link org.catma.queryengine.result.LazyQueryResultRow}s by
 * a call to the {@link org.catma.queryengine.result.LazyQueryResultRow#getTermInfoList()}.
 * Other {@link org.catma.queryengine.result.QueryResultRow}s are not affected neither do
 * any harm.
 * 
 * @author Marco Petris
 * @see org.catma.indexer.Index#getWordlist()
 */
public class WordlistQueryJob extends DefaultProgressCallable<QueryResult> {

    private QueryResult wordlistResult;
    private String jobName;

    /**
     * Constructor.
     * @param wordlistResult a result set that needs to be initialized and typically is a subset
     * of the return value of {@link org.catma.indexer.Index#getWordlist()}.
     * @param jobName the name of the job, that can be displayed to the user
     */
    public WordlistQueryJob(QueryResult wordlistResult, String jobName) {
        this.wordlistResult = wordlistResult;
        this.jobName = jobName;
    }

    public QueryResult call() throws Exception {
        getProgressListener().setIndeterminate(true, jobName);
        
        for(QueryResultRow row : wordlistResult) {
            // lazy initialization
            row.getTermInfoList();
        }
        getProgressListener().setIndeterminate(false, jobName);
        
        return wordlistResult;
    }
}
