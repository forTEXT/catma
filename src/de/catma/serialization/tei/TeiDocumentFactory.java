package de.catma.serialization.tei;

import java.io.IOException;
import java.io.InputStream;

import nu.xom.Builder;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

public class TeiDocumentFactory {

	public TeiDocument createDocumentFromStream(InputStream inputStream) 
			throws ValidityException, ParsingException, IOException {
		Builder builder = new Builder( new TeiNodeFactory() );
		
		TeiDocument teiDocument =  new TeiDocument(builder.build(inputStream));
		TeiDocumentVersion.convertToLatest(teiDocument);
		return teiDocument;
	}
}
