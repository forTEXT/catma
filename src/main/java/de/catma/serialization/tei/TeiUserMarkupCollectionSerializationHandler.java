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

import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.source.SourceDocument;
import de.catma.serialization.DocumentSerializer;
import de.catma.serialization.AnnotationCollectionSerializationHandler;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;

public class TeiUserMarkupCollectionSerializationHandler implements
		AnnotationCollectionSerializationHandler {

	private TagManager tagManager;
	private boolean withText;
	private String version;
	
	public TeiUserMarkupCollectionSerializationHandler(
			TagManager tagManager, String version, boolean withText) {
		super();
		this.tagManager = tagManager;
		this.version = version;
		this.withText = withText;
	}

	public void serialize(
		AnnotationCollection userMarkupCollection,
		SourceDocument sourceDocument,
		OutputStream outputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			TeiDocument teiDocument = 
					factory.createEmptyDocument(userMarkupCollection.getId());
			
			teiDocument.getTeiHeader().setValues(
				userMarkupCollection.getContentInfoSet());
			
			new TeiTagLibrarySerializationHandler(
					teiDocument, tagManager, version).serialize(
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

	public AnnotationCollection deserialize(
			SourceDocument sourceDocument,
			String id, InputStream inputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			TeiDocument teiDocument = 
					factory.createDocumentFromStream(id, inputStream);
			TagLibrary tagLibrary = 
					new TeiTagLibrarySerializationHandler(
							teiDocument, tagManager, version).deserialize();
			
			TeiUserMarkupCollectionDeserializer deserializer = 
					new TeiUserMarkupCollectionDeserializer(
							teiDocument, tagLibrary, id);
			return new AnnotationCollection(
				id, teiDocument.getContentInfoSet(),
				tagLibrary, deserializer.getTagReferences(),
				sourceDocument.getUuid(),
				null,
				null);
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}
	}


}
