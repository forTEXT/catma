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

import de.catma.serialization.TagLibrarySerializationHandler;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;

public class TeiTagLibrarySerializationHandler implements TagLibrarySerializationHandler {
	
	private TagManager tagManager;
	private TeiDocument teiDocument;
	private String version;
	
	public TeiTagLibrarySerializationHandler(
			TeiDocument teiDocument, TagManager tagManager, String version) {
		super();
		this.tagManager = tagManager;
		this.teiDocument = teiDocument;
		this.version = version;
	}

	public TeiTagLibrarySerializationHandler(TagManager tagManager, String version) {
		this(null, tagManager, version);
	}
	
	public void serialize(TagLibrary tagLibrary) {
		TeiTagLibrarySerializer serializer = 
				new TeiTagLibrarySerializer(teiDocument, version);
		serializer.serialize(tagLibrary);
	}
	
	
	public TagLibrary deserialize() throws IOException {
		TeiTagLibraryDeserializer deserializer = 
				new TeiTagLibraryDeserializer(teiDocument, tagManager);
		return tagManager.getTagLibrary();
	}
	
	public TeiDocument getTeiDocument() {
		return teiDocument;
	}
	
	public TagLibrary deserialize(
			String id, InputStream inputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			teiDocument = 
					factory.createDocumentFromStream(id, inputStream);
			return deserialize();
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}
	}
	
}
