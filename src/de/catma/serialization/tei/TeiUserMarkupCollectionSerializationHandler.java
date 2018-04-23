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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.catma.document.AccessMode;
import de.catma.document.source.SourceDocument;
import de.catma.document.standoffmarkup.usermarkup.UserMarkupCollection;
import de.catma.serialization.DocumentSerializer;
import de.catma.serialization.UserMarkupCollectionSerializationHandler;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;

public class TeiUserMarkupCollectionSerializationHandler implements
		UserMarkupCollectionSerializationHandler {

	private TagManager tagManager;
	private boolean withText;
	
	public TeiUserMarkupCollectionSerializationHandler(
			TagManager tagManager, boolean withText) {
		super();
		this.tagManager = tagManager;
		this.withText = withText;
	}

	public void serialize(
		UserMarkupCollection userMarkupCollection,
		SourceDocument sourceDocument,
		OutputStream outputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			TeiDocument teiDocument = 
					factory.createEmptyDocument(userMarkupCollection.getId());
			
			teiDocument.getTeiHeader().setValues(
				userMarkupCollection.getContentInfoSet());
			
			new TeiTagLibrarySerializationHandler(
					teiDocument, tagManager).serialize(
							userMarkupCollection.getTagLibrary());
    		
			new TeiUserMarkupCollectionSerializer(teiDocument, withText).serialize(
    				userMarkupCollection, sourceDocument);
			
			new DocumentSerializer().serialize(
				teiDocument.getDocument(), outputStream);
		}
		catch (Exception exc) {
			throw new IOException(exc);
		}
	}

	public UserMarkupCollection deserialize(
			String id, InputStream inputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			TeiDocument teiDocument = 
					factory.createDocumentFromStream(id, inputStream);
			TagLibrary tagLibrary = 
					new TeiTagLibrarySerializationHandler(
							teiDocument, tagManager).deserialize();
			
			TeiUserMarkupCollectionDeserializer deserializer = 
					new TeiUserMarkupCollectionDeserializer(
							teiDocument, tagLibrary, id);
			return new UserMarkupCollection(
				id, teiDocument.getContentInfoSet(),
				tagLibrary, deserializer.getTagReferences(),
				AccessMode.WRITE);
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}
	}


}
