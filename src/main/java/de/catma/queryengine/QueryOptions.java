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

import de.catma.document.repository.Repository;
import de.catma.indexer.IndexedRepository;
import de.catma.indexer.Indexer;

public class QueryOptions {
	
	private List<String> relevantSourceDocumentIDs;
	private List<String> relevantUserMarkupCollIDs;
	private List<String> relevantStaticMarkupCollIDs;
	private List<String> unseparableCharacterSequences;
	private List<Character> userDefinedSeparatingCharacters;
	private Locale locale;
	private IndexedRepository repository;
	private int limit = 0;
	
	public QueryOptions(List<String> relevantSourceDocumentIDs,
			List<String> relevantUserMarkupCollIDs,
			List<String> relevantStaticMarkupCollIDs,
			List<String> unseparableCharacterSequences,
			List<Character> userDefinedSeparatingCharacters, Locale locale,
			IndexedRepository repository) {
		this.relevantSourceDocumentIDs = relevantSourceDocumentIDs;
		this.relevantUserMarkupCollIDs = relevantUserMarkupCollIDs;
		this.relevantStaticMarkupCollIDs = relevantStaticMarkupCollIDs;
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
	
	public List<String> getRelevantStaticMarkupCollIDs() {
		return relevantStaticMarkupCollIDs;
	}
	
	public List<String> getRelevantUserMarkupCollIDs() {
		return relevantUserMarkupCollIDs;
	}
	
	public Repository getRepository() {
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
}
