package de.catma.core.document.source;

import java.net.URI;
import java.nio.charset.Charset;

public class TechInfoSet {

	private FileType fileType;
	private Charset charset;
	private FileOSType fileOSType;
	private URI uri;
	private Long checksum;
	
	private String xsltDocument; 
	
	public TechInfoSet(FileType fileType, Charset charset,
			FileOSType fileOSType, URI uri, Long checksum) {
		super();
		this.fileType = fileType;
		this.charset = charset;
		this.fileOSType = fileOSType;
		this.uri = uri;
		this.checksum = checksum;
	}
	public FileType getFileType() {
		return fileType;
	}
	public Charset getCharset() {
		return charset;
	}
	public FileOSType getFileOSType() {
		return fileOSType;
	}
	public URI getUri() {
		return uri;
	}
	public Long getChecksum() {
		return checksum;
	}
	
	
	
}
