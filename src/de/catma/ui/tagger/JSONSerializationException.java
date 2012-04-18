package de.catma.ui.tagger;

// somehow the JSONExceptions doesn't work
public class JSONSerializationException extends Exception {

	public JSONSerializationException(Throwable cause) {
		super(cause);
	}
	
}