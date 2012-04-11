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

import de.catma.backgroundservice.BackgroundService;
import de.catma.backgroundservice.ExecutionListener;

/**
 * Executes a {@link org.catma.queryengine.Query} via
 * the {@link org.catma.backgroundservice.BackgroundService}.
 *
 * @author Marco Petris
 *
 */
public class QueryHandler {

    /**
     * Executes the given query via a {@link org.catma.queryengine.QueryJob}.
     * @param inputQuery the string of the query
     * @param execListener this listener will be notified once the query has been completed
     * @param jobName the name of the job, that can be displayed to the user
     * @see org.catma.queryengine.QueryJob
     */
//    public void executeQuery(
//            String inputQuery, ExecutionListener<QueryResult> execListener, String jobName) {
//        BackgroundService.SINGLETON.submit(
//                new QueryJob(inputQuery, jobName), execListener);
//    }

    /**
     * The not yet initialized query result gets initialized by a {@link org.catma.queryengine.WordlistQueryJob}.
     * @param wordlistResult the uninitialized query result
     * @param execListener this listener will be notified once the initialization has been completed
     * @param jobName  the name of the job, that can be displayed to the user
     * @see org.catma.queryengine.WordlistQueryJob
     */
//    public void executeQuery(
//            QueryResult wordlistResult, ExecutionListener<QueryResult> execListener, String jobName) {
//        BackgroundService.SINGLETON.submit(
//                new WordlistQueryJob(wordlistResult, jobName), execListener);
//    }

}
