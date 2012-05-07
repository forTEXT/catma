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

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.antlr.runtime.tree.Tree;

import de.catma.backgroundservice.DefaultProgressCallable;
import de.catma.core.ExceptionHandler;
import de.catma.indexer.Indexer;
import de.catma.queryengine.parser.CatmaQueryLexer;
import de.catma.queryengine.parser.CatmaQueryParser;
import de.catma.queryengine.parser.CatmaQueryWalker;
import de.catma.queryengine.result.QueryResult;


/**
 * A job for the {@link org.catma.backgroundservice.BackgroundService} that executes
 * queris.
 *
 * @author Marco Petris
 *
 */
public class QueryJob extends DefaultProgressCallable<QueryResult> {
    private String inputQuery;
	private Indexer indexer;
	private QueryOptions queryOptions;

    /**
     * Constructor.
     * @param inputQuery the query string
     * @param jobName the name of the job, that can be displayed to the user
     */
    public QueryJob(String inputQuery, Indexer indexer, QueryOptions queryOptions ) {
        this.inputQuery = inputQuery;
        this.indexer = indexer;
        this.queryOptions = queryOptions;
    }

    public QueryResult call() throws Exception {
    	getProgressListener().setProgress("Executing Query...");
        try {
            // parse the query
            CatmaQueryLexer lex =
                new CatmaQueryLexer(new ANTLRStringStream(inputQuery));
            CommonTokenStream tokens = new CommonTokenStream(lex);

            CatmaQueryParser parser = new CatmaQueryParser(tokens);
            CatmaQueryParser.start_return result = parser.start();
            Tree t = (Tree)result.getTree();

            CommonTreeNodeStream nodes = new CommonTreeNodeStream(t);
            nodes.setTokenStream(parser.getTokenStream());

            CatmaQueryWalker walker = new CatmaQueryWalker(nodes);
            Query query = walker.start();
            query.setIndexer(indexer);
            query.setQueryOptions(queryOptions);
            
            // execute the query and retrieve the execution result
            QueryResult queryResult = query.getResult();
            
            getProgressListener().setProgress("Query finished!");
            
            return queryResult;
        }
        catch (Throwable t) {
            if (t instanceof RecognitionException) {
                //noinspection ThrowableInstanceNeverThrown
                ExceptionHandler.log(
                    new QueryException(inputQuery,(RecognitionException)t));
            }
            else if ((t.getCause() != null) && (t.getCause() instanceof RecognitionException)) {
                //noinspection ThrowableInstanceNeverThrown
                ExceptionHandler.log(
                    new QueryException(inputQuery,(RecognitionException)t.getCause()));
            }
            else {
                ExceptionHandler.log(t);
            }
            return null;
        }
    }

    /**
     * An exception that occurred during parsing.
     */
    public class QueryException extends Exception {

        private String input;

        /**
         * Constructor
         * @param input the input to the parser
         * @param re the exception from the parser (which will be {@link #getCause() cause}
         * for this exception)
         */
        public QueryException(String input, RecognitionException re) {
            super(re);
            this.input = input;
        }

        /**
         * @return the input to the parser, that caused the exception
         */
        public String getInput() {
            return input;
        }
    }
}
