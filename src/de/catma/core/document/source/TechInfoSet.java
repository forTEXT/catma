package de.catma.core.document.source;

import java.net.URI;
import java.nio.charset.Charset;

public class TechInfoSet {

	private FileType fileType;
	private Charset charset;
	private FileOSType fileOSType;
	private Long checksum;
	
	private URI uri;
	private String xsltDocument; 
	
	public TechInfoSet(FileType fileType, Charset charset,
			FileOSType fileOSType, Long checksum) {
		super();
		this.fileType = fileType;
		this.charset = charset;
		this.fileOSType = fileOSType;
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
