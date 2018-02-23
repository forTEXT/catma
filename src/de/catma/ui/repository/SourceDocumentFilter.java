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
package de.catma.ui.repository;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.data.Container.Filter;
import com.vaadin.data.Item;

import de.catma.document.Corpus;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollectionReference;

class SourceDocumentFilter implements Filter {
	
	private Set<String> corpusContent;
	
	public SourceDocumentFilter(Corpus corpus) {
		super();
		corpusContent = new HashSet<String>();
		for (SourceDocument sd : corpus.getSourceDocuments()) {
			corpusContent.add(sd.getID());
		}

		for (UserMarkupCollectionReference ucr : corpus.getUserMarkupCollectionRefs()) {
			corpusContent.add(ucr.getId());
		}
	}

	public boolean appliesToProperty(Object propertyId) {
		return true;
	}
	
	public boolean passesFilter(Object itemId, Item item)
			throws UnsupportedOperationException {
		if (itemId instanceof MarkupCollectionItem) {
			return corpusContent.contains(
					((MarkupCollectionItem)itemId).getParentId());
		}
		else if (itemId instanceof SourceDocument){
			return corpusContent.contains(
					((SourceDocument)itemId).getID());
		}
		else if (itemId instanceof UserMarkupCollectionReference) {
			return corpusContent.contains(
					((UserMarkupCollectionReference)itemId).getId());
		}
		
		return false;
	}
	
	
}