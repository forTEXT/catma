package de.catma.core.document.source;

import java.net.URI;

public class SourceDocumentInfo {

	private IndexInfoSet indexInfoSet;
	private ContentInfoSet contentInfoSet;
	private TechInfoSet techInfoSet;
	private URI uri;
	private String mimeType;
	
	public SourceDocumentInfo() {
	}
	
	public SourceDocumentInfo(IndexInfoSet indexInfoSet,
			ContentInfoSet contentInfoSet, TechInfoSet techInfoSet) {
		super();
		this.indexInfoSet = indexInfoSet;
		this.contentInfoSet = contentInfoSet;
		this.techInfoSet = techInfoSet;
	}
	public IndexInfoSet getIndexInfoSet() {
		return indexInfoSet;
	}
	public ContentInfoSet getContentInfoSet() {
		return contentInfoSet;
	}
	public TechInfoSet getTechInfoSet() {
		return techInfoSet;
	}
	
	public void setURI(URI sourceURI) {
		this.uri=sourceURI;
	}
	
	public URI getURI() {
		return uri;
	}
	
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	
	public String getMimeType() {
		return mimeType;
	}
	
}
