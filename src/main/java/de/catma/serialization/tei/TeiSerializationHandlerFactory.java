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
package de.catma.serialization.tei;

import de.catma.serialization.SerializationHandlerFactory;
import de.catma.serialization.SourceDocumentInfoSerializationHandler;
import de.catma.serialization.TagLibrarySerializationHandler;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;
import de.catma.tag.TagManager;

public class TeiSerializationHandlerFactory implements
		SerializationHandlerFactory {
	
	private TagManager tagManager;

	public void setTagManager(TagManager tagManager) {
		this.tagManager = tagManager;
	}

	public SourceDocumentInfoSerializationHandler getSourceDocumentInfoSerializationHandler() {
		return new TeiSourceDocumentInfoSerializationHandler();
	}

	public TagLibrarySerializationHandler getTagLibrarySerializationHandler() {
		return new TeiTagLibrarySerializationHandler(tagManager);
	}

	public UserMarkupCollectionSerializationHandler getUserMarkupCollectionSerializationHandler() {
		return new TeiUserMarkupCollectionSerializationHandler(tagManager, false);
	}

}
