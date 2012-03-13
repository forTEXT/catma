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
				Thread.currentThread().getContextClassLoader().getResourceAsStream(
						"de/catma/serialization/tei/MinimalStandoffmarkup.xml"));
		//TODO: set Version to 3 to prevent senseless version upgrade
	}
}
