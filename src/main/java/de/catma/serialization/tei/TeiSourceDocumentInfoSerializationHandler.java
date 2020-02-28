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

import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.IndexInfoSet;
import de.catma.document.source.SourceDocumentInfo;
import de.catma.document.source.TechInfoSet;
import de.catma.serialization.DocumentSerializer;
import de.catma.serialization.SourceDocumentInfoSerializationHandler;

public class TeiSourceDocumentInfoSerializationHandler implements
		SourceDocumentInfoSerializationHandler {

	public SourceDocumentInfo deserialize(
			String id, InputStream inputStream) throws IOException {
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			TeiDocument teiDocument = 
					factory.createDocumentFromStream(id, inputStream);
			
			return deserialize(teiDocument);
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}	
	}

	private SourceDocumentInfo deserialize(TeiDocument teiDocument) {
		ContentInfoSet contentInfoSet = teiDocument.getContentInfoSet();	
		TechInfoSet techInfoSet = teiDocument.getTechInfoset();
		IndexInfoSet indexInfoSet = teiDocument.getIndexInfoSet();
		
		return new SourceDocumentInfo(indexInfoSet, contentInfoSet, techInfoSet);
	}

	public void serialize(
			SourceDocument sourceDocument, 
			OutputStream outputStream) throws IOException {
		
		try {
			TeiDocumentFactory factory = new TeiDocumentFactory();
			
			TeiDocument teiDocument = factory.createEmptyDocument(
					sourceDocument.getUuid());
			SourceDocumentInfo sourceDocumentInfo = 
					sourceDocument.getSourceContentHandler().getSourceDocumentInfo();
			
			teiDocument.getTeiHeader().setValues(
				sourceDocumentInfo.getContentInfoSet(), 
				sourceDocumentInfo.getTechInfoSet(),
				sourceDocumentInfo.getIndexInfoSet());
			
			DocumentSerializer serializer = new DocumentSerializer();
			serializer.serialize(teiDocument.getDocument(), outputStream);
		} catch (Exception exc) {
			throw new IOException(exc);
		}	
	}

}
