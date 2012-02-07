package de.catma.core.document.source;

import java.net.URI;
import java.nio.charset.Charset;

public class TechInfoSet {

	private FileType fileType;
	private Charset charset;
	private FileOSType fileOSType;
	private Long checksum;
	private String mimeType;
	private URI uri;
	private String xsltDocumentName;
	private boolean managedResource; 
	
	public TechInfoSet(FileType fileType, Charset charset,
			FileOSType fileOSType, Long checksum, String xsltDocumentName) {
		super();
		this.fileType = fileType;
		this.charset = charset;
		this.fileOSType = fileOSType;
		this.checksum = checksum;
		this.xsltDocumentName = xsltDocumentName;
	}
	
	public TechInfoSet(String mimeType, URI uri) {
		super();
		this.mimeType = mimeType;
		this.uri = uri;
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
	
	public URI getURI() {
		return uri;
	}
	
	public Long getChecksum() {
		return checksum;
	}
	
	public String getMimeType() {
		return mimeType;
	}
	
	public void setFileOSType(FileOSType fileOSType) {
		this.fileOSType = fileOSType;
	}
	
	public void setFileType(FileType fileType) {
		this.fileType = fileType;
	}
	
	public void setCharset(Charset charset) {
		this.charset = charset;
	}
	
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	public void setChecksum(Long checksum) {
		this.checksum = checksum;
	}
	
	public void setURI(URI uri) {
		this.uri = uri;
	}
	
	public String getXsltDocumentName() {
		return xsltDocumentName;
	}
	
	public void setXsltDocumentName(String xsltDocumentName) {
		this.xsltDocumentName = xsltDocumentName;
	}

	public void setManagedResource(boolean managedResource) {
		this.managedResource = managedResource;
	}
	
	public boolean isManagedResource() {
		return managedResource;
	}
}
