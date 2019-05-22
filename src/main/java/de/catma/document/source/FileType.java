/*   
 *   CATMA Computer Aided Text Markup and Analysis
 *   
 *   Copyright (C) 2009  University Of Hamburg
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The type of a file: pdf, html, text...
 *
 * @author marco.petris@web.de
 *
 */
public enum FileType {
	/**
	 * MS-Word DOCs.
	 */
	DOC("doc", "application/msword"), 
	/**
	 * PDFs.
	 */
	PDF("pdf", "application/pdf"), 
	XPDF("xpdf", "application/x-pdf"),
	/**
	 * HTML-pages.
	 */
	HTML("html", true, "text/html"),
    /**
     * HTM(L)-pages.
     */
    HTM("htm", true, "text/html"),
    /**
     * RTF-docs.
     */
    RTF("rtf", "application/rtf"),
	/**
	 * everything which is not one of the other possibilities
	 */
	TEXT("txt", true, "text/plain"),
	/**
	 * XML files.
	 */
	XML("xml", false,false, "application/xml", "text/xml"), // not active since empty element handling was flawed
	XML2("xml", true, "application/xml", "text/xml"), 
	TEI("xml", false, false, "application/tei+xml"), // not active since support would require a proper way to display structural elements and their customizations
	/**
	 * MS-Word DOCX files.
	 */
	DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
	/**
	 * ZIP files.
	 */
	ZIP("zip", "application/zip")
	;
	
	private String[] mimeTypes;
	private boolean active;
	private boolean charsetSupported;
	private String defaultExtension;
	
	private FileType(String defaultExtension, String... mimeTypes) {
		this(defaultExtension, true, false, mimeTypes);
	}	

	private FileType(String defaultExtension, boolean supportsCharset, String... mimeTypes) {
		this(defaultExtension, true, supportsCharset, mimeTypes);
	}	
	
	private FileType(String defaultExtension, boolean active, boolean supportsCharset, String... mimeTypes) {
		this.defaultExtension = defaultExtension;
		this.mimeTypes = mimeTypes;
		this.active = active;
		this.charsetSupported = supportsCharset;
	}
	
	/**
	 * @return the mime type of this file type.
	 */
	public String getMimeType() {
		return mimeTypes[0];
	}
	
	public boolean hasMimeType(String mimeTypeToTest) {
		for (String mimeType : mimeTypes) {
			if (mimeType.equals(mimeTypeToTest)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isCharsetSupported() {
		return charsetSupported;
	}
	
	/**
	 * Tries to guess the file type by analyzing the file extension i. e. the 
	 * characters after the last dot.
	 * @param file the file to analyze
	 * @return the type of the file
	 */
	static FileType getFileType( File file ) {
		return getFileTypeFromName(file.getName());
	}
	
	/**
	 * Tries to guess the file type by analyzing the file extension i. e. the 
	 * characters after the last dot.
	 * @param fileName the filename to analyze
	 * @return the type of the file
	 */
	public static FileType getFileTypeFromName( String fileName ) {
		int indexOflastDot = fileName.lastIndexOf( '.' );

		if ((indexOflastDot != -1) && (indexOflastDot<(fileName.length()-1))){
			String extension = 
					fileName.substring( indexOflastDot+1 ).toUpperCase();
			for (FileType type : values()) {
                if( type.active && extension.equals( type.defaultExtension.toUpperCase() ) ) {
                    return type;
                }
            }
        }		
		return TEXT;
	}
	
	/**
	 * @param mimeType
	 * @return the file type that corresponds to the given mime type or, if no specific
	 * file type can be found, {@link #TEXT}.
	 */
	public static FileType getFileType(String mimeType) {
		for (FileType type : values()) {
			if (type.active && type.hasMimeType(mimeType)) {
				return type;
			}
		}
		return TEXT;
	}
	
	public static List<FileType> getActiveFileTypes() {
		List<FileType> result = new ArrayList<FileType>();
		for (FileType type : values()) {
			if (type.active) {
				result.add(type);
			}
		}
		return result;
	}
	
	public String getDefaultExtension() {
		return defaultExtension;
	}
}
