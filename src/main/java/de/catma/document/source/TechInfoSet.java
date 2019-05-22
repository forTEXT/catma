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
package de.catma.document.source;

import java.net.URI;
import java.nio.charset.Charset;

/**
 * Technical metadata about the physical source.
 * 
 * @author marco.petris@web.de
 *
 */
public class TechInfoSet {

	private String fileName;
	private FileType fileType;
	private Charset charset;
	private FileOSType fileOSType;
	private Long checksum;
	private String mimeType;
	private URI uri;
	
	public TechInfoSet() {
		
	}
	
	/**
	 * Copy constructor
	 * @param another the TechInfoSet object to copy
	 */
	public TechInfoSet(TechInfoSet another) {
		this.fileName = another.fileName;
		this.fileType = another.fileType;
		this.charset = another.charset;
		this.fileOSType = another.fileOSType;
		this.checksum = another.checksum;
		this.mimeType = another.mimeType;
		this.uri = another.uri;
	}
	
	/**
	 * @param fileType the type of the file
	 * @param charset the charset used in the file
	 * @param fileOSType determines line endings 
	 * @param checksum a checksum of the content
	 */
	public TechInfoSet(FileType fileType, Charset charset,
			FileOSType fileOSType, Long checksum) {
		super();
		this.fileType = fileType;
		this.charset = charset;
		this.fileOSType = fileOSType;
		this.checksum = checksum;
	}
	
	/**
	 * @param fileName name of the file
	 * @param mimeType mimetype of the file
	 * @param uri source address
	 */
	public TechInfoSet(String fileName, String mimeType, URI uri) {
		this.fileName = fileName;
		this.mimeType = mimeType;
		this.uri = uri;
	}
	
	public String getFileName() {
		return fileName;
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
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
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

}
