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
package de.catma.indexer;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Attribute;

public class PeekableTokenStream {

	private TokenStream tokenStream;
	
	private CharTermAttribute termAttrBuffer;
	private OffsetAttribute offsetAttrBuffer;
	private boolean bufferFull;
	private boolean canPeek;
	private boolean init;

	public PeekableTokenStream(TokenStream tokenStream) throws IOException {
		this.tokenStream = tokenStream;
		this.tokenStream.reset();
		init = false;
	}

	private void fillBuffer() throws IOException {
		
		if (tokenStream.incrementToken()) {
			termAttrBuffer =
					new ReadOnlyCharTermAttribute(
							tokenStream.getAttribute(CharTermAttribute.class));
			offsetAttrBuffer = 
					new ReadOnlyOffsetAttribute(
							tokenStream.getAttribute(OffsetAttribute.class));
			canPeek = tokenStream.incrementToken();
			bufferFull = true;

		}
		else {
			bufferFull = false;
			canPeek = false;
		}
		
		init = true;
	}

	@SuppressWarnings("unchecked")
	public <A extends Attribute> A getAttribute(Class<A> attClass) {
		
		if (!bufferFull) {
			throw new IllegalStateException("No more tokens in stream");
		}
		
		if (attClass.equals(CharTermAttribute.class)) {
			return (A)termAttrBuffer;
		}
		else if (attClass.equals(OffsetAttribute.class)){
			return (A)offsetAttrBuffer;
		}
		else {
			throw new IllegalArgumentException(
				"Cannot provide attribute "
			+ attClass +" in PeekableTokenStream");
		}
	}
	
	
	public boolean incrementToken() throws IOException {
		if (!init) {
			fillBuffer();
		}
		else {
			if (canPeek) {
				termAttrBuffer =
						new ReadOnlyCharTermAttribute(
								tokenStream.getAttribute(CharTermAttribute.class));
				offsetAttrBuffer = 
						new ReadOnlyOffsetAttribute(
								tokenStream.getAttribute(OffsetAttribute.class));
				bufferFull = true;
				canPeek = tokenStream.incrementToken();
			}
			else {
				bufferFull = false;
			}
		}
		return bufferFull;
	}

	
	public boolean canPeek() {
		return canPeek;
	}
	
	public <A extends Attribute> A peekAttribute(Class<A> attClass) {
		if (!canPeek) {
			throw new IllegalStateException(
					"No more tokens in stream, cannot peek");
		}
		else {
			return tokenStream.getAttribute(attClass);
		}
	}
}
