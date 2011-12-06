package de.catma.serialization.tei;

import java.io.IOException;
import java.io.InputStream;

import nu.xom.Builder;
import nu.xom.ParsingException;
import nu.xom.ValidityException;
import de.catma.core.tag.TagIDGenerator;
import de.catma.core.tag.TagLibrary;
import de.catma.serialization.TagLibrarySerializer;

public class TeiTagLibrarySerializer implements TagLibrarySerializer {

	public void serialize(TagLibrary tagLibrary) {
		// TODO Auto-generated method stub

	}

	public TagLibrary deserialize(InputStream inputStream) throws IOException {
		try {
			TeiDocument teiDocument = createDocumentFromStream(inputStream);
			TeiDocumentVersion.convertToLatest(teiDocument);
			
			
			
		} catch (Exception exc) {
			throw new IOException(exc);
		}
		
		return null;
	}
	
	private TeiDocument createDocumentFromStream(InputStream inputStream) 
			throws ValidityException, ParsingException, IOException {
		Builder builder = new Builder( new TeiNodeFactory() );
		
		return new TeiDocument(builder.build(inputStream));
	}
	
	/**
	 * @return a new {@link Attribute#xmlid}
	 */
	public static nu.xom.Attribute getNewXmlIDAttribute() {
		return new nu.xom.Attribute( 
				Attribute.xmlid.getPrefixedName(),
				Attribute.xmlid.getNamespaceURI(),
				new TagIDGenerator().generate() );
	}
	

}
