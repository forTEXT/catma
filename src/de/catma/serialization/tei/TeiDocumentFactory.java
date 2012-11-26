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
