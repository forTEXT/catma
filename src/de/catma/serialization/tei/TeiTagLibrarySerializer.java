package de.catma.serialization.tei;

import java.io.IOException;
import java.io.InputStream;

import nu.xom.Builder;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.ValidityException;
import de.catma.core.tag.TagIDGenerator;
import de.catma.core.tag.TagLibrary;
import de.catma.core.tag.TagsetDefinition;
import de.catma.core.tag.Version;
import de.catma.serialization.TagLibrarySerializer;

public class TeiTagLibrarySerializer implements TagLibrarySerializer {

	public void serialize(TagLibrary tagLibrary) {
		// TODO Auto-generated method stub

	}

	public TagLibrary deserialize(InputStream inputStream) throws IOException {
		try {
			TeiDocument teiDocument = createDocumentFromStream(inputStream);
			TeiDocumentVersion.convertToLatest(teiDocument);
			
			Nodes tagsetDefinitionElements = teiDocument.getTagLibraryElements();
			
			for (int i=0; i<tagsetDefinitionElements.size(); i++) {
				TeiElement tagsetDefinition = (TeiElement)tagsetDefinitionElements.get(i);
				String nValue = tagsetDefinition.getAttributeValue(Attribute.n);
				int dividerPos = nValue.lastIndexOf(' ');
				String tagsetName = nValue.substring(0, dividerPos);
				String versionString = nValue.substring(dividerPos+1);
				TagsetDefinition td = 
						new TagsetDefinition(
								tagsetDefinition.getID(),tagsetName, new Version(versionString));
				
				System.out.println(td);
			}
			
			
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
