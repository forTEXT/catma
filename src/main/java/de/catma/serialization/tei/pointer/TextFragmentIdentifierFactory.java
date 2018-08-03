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
