package de.catma.indexer;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Attribute;

public class PeekableTokenStream extends TokenStream {

	private TokenStream tokenStream;
	
	private CharTermAttribute termAttrBuffer;
	private OffsetAttribute offsetAttrBuffer;
	private boolean bufferFull;
	private boolean canPeek;
	private boolean init;

	public PeekableTokenStream(TokenStream tokenStream) throws IOException {
		this.tokenStream = tokenStream;
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
			throw new IllegalStateException("no more tokens in stream");
		}
		
		if (attClass.equals(CharTermAttribute.class)) {
			return (A)termAttrBuffer;
		}
		else if (attClass.equals(OffsetAttribute.class)){
			return (A)offsetAttrBuffer;
		}
		else {
			throw new IllegalArgumentException(
				"cannot provide attribute " 
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
					"no more tokens in stream, cannot peek");
		}
		else {
			return tokenStream.getAttribute(attClass);
		}
	}
}
