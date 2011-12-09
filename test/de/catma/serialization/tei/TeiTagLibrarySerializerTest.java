package de.catma.serialization.tei;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Test;

import de.catma.core.document.source.contenthandler.BOMFilterInputStream;

public class TeiTagLibrarySerializerTest {

	@Test
	public void test() throws FileNotFoundException, IOException {
		FilterInputStream is = new BOMFilterInputStream(
				new FileInputStream("testdocs/rose_for_emily_user_simple.xml"), Charset.forName( "UTF-8" ));
		new TeiTagLibrarySerializationHandler().deserialize(is);
	}

}
