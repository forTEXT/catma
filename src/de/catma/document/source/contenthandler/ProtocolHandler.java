package de.catma.document.source.contenthandler;

public interface ProtocolHandler {
	public byte[] getByteContent();
	public String getEncoding();
	public String getMimeType();
}
