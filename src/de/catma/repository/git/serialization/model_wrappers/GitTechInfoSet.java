package de.catma.repository.git.serialization.model_wrappers;

import com.jsoniter.annotation.JsonIgnore;
import de.catma.document.source.FileOSType;
import de.catma.document.source.FileType;
import de.catma.document.source.TechInfoSet;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

public class GitTechInfoSet {
	private TechInfoSet techInfoSet;

	public GitTechInfoSet() {
		this.techInfoSet = new TechInfoSet();
	}

	public GitTechInfoSet(TechInfoSet techInfoSet) {
		this.techInfoSet = techInfoSet;
	}

	@JsonIgnore
	public TechInfoSet getTechInfoSet() {
		return this.techInfoSet;
	}

	public String getFileName() {
		return this.techInfoSet.getFileName();
	}

	public void setFileName(String fileName) {
		this.techInfoSet.setFileName(fileName);
	}

	public FileType getFileType() {
		return this.techInfoSet.getFileType();
	}

	public void setFileType(FileType fileType) {
		this.techInfoSet.setFileType(fileType);
	}

	public String getCharset() {
		return this.techInfoSet.getCharset().name();
	}

	public void setCharset(String charsetName) {
		this.techInfoSet.setCharset(Charset.forName(charsetName));
	}

	public FileOSType getFileOSType() {
		return this.techInfoSet.getFileOSType();
	}

	public void setFileOSType(FileOSType fileOSType) {
		this.techInfoSet.setFileOSType(fileOSType);
	}

	public Long getChecksum() {
		return this.techInfoSet.getChecksum();
	}

	public void setChecksum(Long checksum) {
		this.techInfoSet.setChecksum(checksum);
	}

	public String getMimeType() {
		return this.techInfoSet.getMimeType();
	}

	public void setMimeType(String mimeType) {
		this.techInfoSet.setMimeType(mimeType);
	}

	public String getURI() {
		URI uri = this.techInfoSet.getURI();
		return uri == null ? null : uri.toString();
	}

	public void setURI(String uri) throws URISyntaxException {
		this.techInfoSet.setURI(uri == null ? null : new URI(uri));
	}

	public String getXsltDocumentLocalUri() {
		return this.techInfoSet.getXsltDocumentLocalUri();
	}

	public void setXsltDocumentLocalUri(String xsltDocumentLocalUri) {
		this.techInfoSet.setXsltDocumentLocalUri(xsltDocumentLocalUri);
	}
}
