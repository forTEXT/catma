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

import de.catma.document.standoffmarkup.staticmarkup.StaticMarkupCollection;
import de.catma.serialization.StaticMarkupCollectionSerializationHandler;

public class TeiStaticMarkupCollectionSerializationHandler implements
		StaticMarkupCollectionSerializationHandler {

	public void serialize(StaticMarkupCollection staticMarkupCollection) {
		// TODO Auto-generated method stub

	}

	public StaticMarkupCollection deserialize(
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

	private StaticMarkupCollection deserialize(TeiDocument teiDocument) {
		// TODO Auto-generated method stub
		return null;
	}

}
