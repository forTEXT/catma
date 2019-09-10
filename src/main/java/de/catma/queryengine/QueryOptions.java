/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009-2013  University Of Hamburg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.catma.queryengine;

import java.util.List;
import java.util.Locale;

import de.catma.indexer.IndexedProject;
import de.catma.indexer.Indexer;
import de.catma.project.Project;

public class QueryOptions {
	
	private List<String> relevantSourceDocumentIDs;
	private List<String> relevantUserMarkupCollIDs;
	private List<String> unseparableCharacterSequences;
	private List<Character> userDefinedSeparatingCharacters;
	private Locale locale;
	private IndexedProject repository;
	private int limit = 0;
	private QueryId queryId;
	
	public QueryOptions(
			QueryId queryId, 
			List<String> relevantSourceDocumentIDs,
			List<String> relevantUserMarkupCollIDs,
			List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale,
			IndexedProject repository) {
		this.queryId = queryId;
		this.relevantSourceDocumentIDs = relevantSourceDocumentIDs;
		this.relevantUserMarkupCollIDs = relevantUserMarkupCollIDs;
		this.unseparableCharacterSequences = unseparableCharacterSequences;
		this.userDefinedSeparatingCharacters = userDefinedSeparatingCharacters;
		this.locale = locale;
		this.repository = repository;
	}

	public List<String> getRelevantSourceDocumentIDs() {
		return relevantSourceDocumentIDs;
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
	
	public List<String> getRelevantUserMarkupCollIDs() {
		return relevantUserMarkupCollIDs;
	}
	
	public Project getRepository() {
		return repository;
	}
	
	public Indexer getIndexer() {
		return repository.getIndexer();
	}
	
	public int getLimit() {
		return limit;
	}
	
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	public QueryId getQueryId() {
		return queryId;
	}
}
