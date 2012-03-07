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

package de.catma.queryengine.result;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import de.catma.core.ExceptionHandler;
import de.catma.indexer.Index;
import de.catma.indexer.TermInfo;


/**
 * A row of {@link org.catma.queryengine.result.QueryResult} that gets initialized lazy by calling
 * {@link #getTermInfoList()}.
 *
 * @author Marco Petris
 *
 */
public class LazyQueryResultRow extends QueryResultRow {

    private int tokenCount;

    /**
     * Constructor.
     * @param text the type of this result row
     * @param tokenCount the token count for the type
     */
    public LazyQueryResultRow(String text, int tokenCount) {
        super(text,null);
        this.tokenCount = tokenCount;
    }


    /**
     * @return the list of tokens (lazy initialized) 
     */
    @Override
    public List<TermInfo> getTermInfoList() {
        if(super.getTermInfoList() == null) {
            List<TermInfo> termInfoList = Collections.emptyList();
            try {
                Index index = FileManager.SINGLETON.getCurrentSourceDocument().getIndex();
                termInfoList = index.search(getText());
            }
            catch(IOException e) {
                ExceptionHandler.log(e);
            }
            setTermInfoList(termInfoList);
        }
        return super.getTermInfoList();
    }

    @Override
    public int getTokenCount() {
        return tokenCount;
    }
}
