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

import nu.xom.Builder;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

public class TeiDocumentFactory {
	
	public TeiDocument createDocumentFromStream(
			String id, InputStream inputStream) 
			throws ValidityException, ParsingException, IOException {
		return createDocumentFromStream(id, inputStream, true);
	}

	private TeiDocument createDocumentFromStream(
			String id, InputStream inputStream, boolean versionUpgrade) 
			throws ValidityException, ParsingException, IOException {
		Builder builder = new Builder( new TeiNodeFactory() );
		
		TeiDocument teiDocument =  new TeiDocument(id, builder.build(inputStream));
		TeiDocumentVersion.convertToLatest(teiDocument);
		teiDocument.hashIDs();
		
		return teiDocument;
	}
	
	public TeiDocument createEmptyDocument(String id) 
			throws ValidityException, ParsingException, IOException {
		return createDocumentFromStream(
				id,
				this.getClass().getResourceAsStream(
						"/de/catma/serialization/tei/MinimalStandoffMarkup.xml"),
				false);
	}
}
