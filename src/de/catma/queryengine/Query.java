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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import de.catma.indexer.Indexer;
import de.catma.indexer.TermInfo;
import de.catma.queryengine.result.ResultList;

/**
 * The base class for all queries.
 *
 * @author Marco Petris <marco.petris@web.de>
 */
public abstract class Query {
	
    private Refinement refinement;
    private Indexer indexer;
	private List<String> unseparableCharacterSequences;
	private List<Character> userDefinedSeparatingCharacters;
	private Locale locale;
	private List<String> documentIds;

    /**
     * Executes a query and returns a {@link org.catma.queryengine.result.ResultList} that has
     * not been refined yet by the {@link org.catma.queryengine.Refinement} of this query.
     * <br><br>
     * Normally this method does not need to be called directly. To execute a query
     * and to retrieve the final execution result you should call {@link #getResult()} instead.
     *
     * @return the <b>unrefined</b> result, never <code>null</code>
     * @throws Exception see instance for details
     */
    protected abstract ResultList execute() throws Exception;

    /**
     * @return the result of the execution, optionally refined by a {@link org.catma.queryengine.Refinement}.
     * @throws Exception see instance for details 
     */
    public ResultList getResult() throws Exception {

        ResultList result = execute();
        
        if(refinement != null) {
            return refinement.refine(result);
        }

        return result;
    }

    /**
     * @param refinement the new refinement for the execution result
     */
    public void setRefinement(Refinement refinement) {
        this.refinement = refinement;
    }

    /**
     * Implementors can provide a special comparator for the {@link org.catma.indexer.TermInfo} of
     * the execution result. This comparator will be used to define the exclusion/refinement condition for
     * {@link org.catma.queryengine.ExclusionQuery}/{@link org.catma.queryengine.Refinement} queries.
     *
     * <b>Node:</b>: these comparators might impose orderings that are inconsistent with equals!!!
     *
     * @return a special comparator or <code>null</code>
     * @see org.catma.queryengine.ExclusionQuery
     * @see org.catma.queryengine.Refinement
     */
    public Comparator<TermInfo> getComparator() {
        return null;
    }
    
    public void setIndexOptions(List<String> unseparableCharacterSequences,
            List<Character> userDefinedSeparatingCharacters,
            Locale locale) {
    	this.unseparableCharacterSequences = unseparableCharacterSequences;
    	this.userDefinedSeparatingCharacters = userDefinedSeparatingCharacters;
    	this.locale = locale;
    }
    
    public void setIndexer(Indexer indexer) {
		this.indexer = indexer;
	}
    
    public Indexer getIndexer() {
		return indexer;
	}
    
    public List<String> getUnseparableCharacterSequences() {
		return unseparableCharacterSequences;
	}
    
    public List<Character> getUserDefinedSeparatingCharacters() {
		return userDefinedSeparatingCharacters;
	}
    
    public Locale getLocale() {
		return locale;
	}
    
    public void setDocumentIds(List<String> documentIds) {
		this.documentIds = documentIds;
	}
    
    public List<String> getDocumentIds() {
		return documentIds;
	}
    
}
