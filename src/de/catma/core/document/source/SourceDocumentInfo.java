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

package de.catma.core.document.source;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.CRC32;

/**
 * Holds information about a SourceDocument like the {@link FileType}, the
 * {@link Charset}, the {@link FileOSType}, the checksum and so on.
 *
 * @author Marco Petris
 *
 */
public class SourceDocumentInfo {

	private FileType fileType;
	private Charset charset;
	private FileOSType fileOSType;
	private String previewContent;
	private String author;
	private String description;
	private String publisher;
	private URI uri;
	private String title;
	
	
	
//	private SourceTextType sourceTextType; //FIXME: 
	private Long checksum;
    private Locale locale;
    private List<String> unseparableCharacterSequences;
    private List<Character> userDefinedSeparatingCharacters;

    /**
	 * Constructor.
	 * @param fileType the type of the SourceDocument
	 * @param uri the URI of the document
	 */
	public SourceDocumentInfo( FileType fileType, 
			URI uri ) {
        this( fileType, FileOSType.INDEPENDENT, Charset.defaultCharset(),
                Locale.getDefault(), null, uri);
	}

	/**
	 * Constructor.
	 * @param fileType the type of the SourceDocument
	 * @param fileOSType the OS type of the file
	 * @param charset the character set of the file
     * @param locale the (main) locale of the content
	 * @param previewContent a preview of the content of the file
	 * @param uri the URI of the document
	 */
	public SourceDocumentInfo( 
			FileType fileType, FileOSType fileOSType, Charset charset,
            Locale locale, String previewContent, URI uri ) {
		this.fileType = fileType;
		this.charset = charset;
        this.locale = locale;
		this.fileOSType = fileOSType;
		this.previewContent = previewContent;
		this.uri = uri;
	}


	/**
	 * @return the charset of the Source Document
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * @param charset the charset of the Source Document
	 */
	public void setCharset( Charset charset ) {
		this.charset = charset;
	}

	/**
	 * @return a preview of the content of the Source Document
	 */
	public String getPreviewContent() {
		return previewContent;
	}

	/**
	 * @param previewContent a preview of the content of the Source Document
	 */
	public void setPreviewContent( String previewContent ) {
		this.previewContent = previewContent;
	}

	/**
	 * @return the file type of the Source Document
	 */
	public FileType getFileType() {
		return fileType;
	}

	/**
	 * @return the file type of the Source Document
	 */
	public FileOSType getFileOSType() {
		return fileOSType;
	}
	
	/**
	 * @param fileOSType the files OS type
	 */
	public void setFileOSType( FileOSType fileOSType ) {
		this.fileOSType = fileOSType;
	}

	/**
	 * @param author the author of the Source Document (TEI-Header)
	 */
	public void setAuthor( String author ) {
		this.author = author;
	}

	/**
	 * @param description the description of the Source Document (TEI-Header)
	 */
	public void setDescription( String description ) {
		this.description = description;
	}

	/**
	 * @param publisher the publisher of the Source Document (TEI-Header)
	 */
	public void setPublisher( String publisher ) {
		this.publisher = publisher;
	}

	/**
	 * @param title the title of the Source Document (TEI-Header)
	 */
	public void setTitle( String title ) {
		this.title = title;
	}
    
	
	/**
	 * @param fileType the file type of the Source Document 
	 */
	public void setFileType( FileType fileType ) {
		this.fileType = fileType;
	}
	
	/**
	 * @param checksum the {@link CRC32}-checksum
	 */
	public void setChecksum( Long checksum ) {
		this.checksum = checksum;
	}
	
	/**
	 * @return the {@link CRC32}-checksum
	 */
	public Long getChecksum() {
		return checksum;
	}

    /**
     * @return the locale of the Source Document
     */
    public Locale getLocale() {
        return (locale==null) ? Locale.getDefault() : locale;
    }

    /**
     * @param locale the locale of the Source Document
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * @return a (possibly empty) list of unseparable character sequences,
     * does not return <oode>null</oode>
     */
    public List<String> getUnseparableCharacterSequences() {
        return (unseparableCharacterSequences==null) ?
                Collections.<String>emptyList() : unseparableCharacterSequences;
    }

    /**
     * @param unseparableCharacterSequences the list of unseparable character sequences
     * (allows <code>null</code>)
     */
    public void setUnseparableCharacterSequences(List<String> unseparableCharacterSequences) {
        this.unseparableCharacterSequences = unseparableCharacterSequences;
    }

    /**
     * @return a (possibly empty) list of user defined speparating character sequences,
     * does not return <oode>null</oode>
     */
    public List<Character> getUserDefinedSeparatingCharacters() {
        return (userDefinedSeparatingCharacters ==null) ?
                Collections.<Character>emptyList() : userDefinedSeparatingCharacters;
    }

    /**
     * @param userDefineedSeparatingCharacters the list of user defined speparating character sequences 
     * (allows <code>null</code>)
     */
    public void setUserDefinedSeparatingCharacters(List<Character> userDefineedSeparatingCharacters) {
        this.userDefinedSeparatingCharacters = userDefineedSeparatingCharacters;
    }

	public URI getURI() {
		return uri;
	}
}
