package de.catma.serialization.tei.pointer;

import java.io.IOException;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.apache.commons.io.IOUtils;

import de.catma.serialization.tei.pointer.parser.URIFragmentIdentifierPlainTextLexer;
import de.catma.serialization.tei.pointer.parser.URIFragmentIdentifierPlainTextParser;

public class TextFragmentIdentifierFactory {

	public TextFragmentIdentifierFactory() {
		
	}
	
	public TextFragmentIdentifier createTextFragmentIdentifier(String fragmentIdentifier) throws RecognitionException {
		ANTLRInputStream is;
		try {
			is = new ANTLRInputStream(IOUtils.toInputStream(fragmentIdentifier));
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		URIFragmentIdentifierPlainTextLexer lexer = new URIFragmentIdentifierPlainTextLexer(is);
		CommonTokenStream ts = new CommonTokenStream(lexer);
		URIFragmentIdentifierPlainTextParser parser = new URIFragmentIdentifierPlainTextParser(ts);
		parser.start();
		return  parser.getTextFragmentIdentifier();

	}
}
