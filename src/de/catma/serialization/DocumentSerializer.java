package de.catma.serialization;

import java.io.IOException;
import java.io.OutputStream;

import nu.xom.Document;
import nu.xom.Serializer;

public class DocumentSerializer {
	public void serialize(Document document, OutputStream outputStream) throws IOException {
//			outputStream.write( BOMFilterInputStream.UTF_8_BOM ); // some jdks do not write it on their own
		Serializer serializer = new Serializer( outputStream );
		serializer.setIndent( 4 );
		serializer.write( document );
	}
}
