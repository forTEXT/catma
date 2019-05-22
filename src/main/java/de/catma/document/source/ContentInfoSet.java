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


/**
 * Bibliographical information.
 * 
 * @author marco.petris@web.de
 *
 */
public class ContentInfoSet {

	private String author;
	private String description;
	private String publisher;
	private String title;
    
	/**
	 * @param author author of the content
	 * @param description description of the content
	 * @param publisher publisher of the content
	 * @param title title of the content
	 */
	public ContentInfoSet(String author, String description, String publisher,
			String title) {
		super();
		this.author = author;
		this.description = description;
		this.publisher = publisher;
		this.title = title;
	}

	/**
	 * Empty bibliographical information.
	 */
	public ContentInfoSet() {
		this.author = "";
		this.description = "";
		this.title = "";
		this.publisher = "";
	}

	/**
	 * @param title title of the content
	 */
	public ContentInfoSet(String title) {
		this("", "", "",title);
	}

	/**
	 * Copy constructor.
	 * @param contentInfoSet to be copied. 
	 */
	public ContentInfoSet(ContentInfoSet contentInfoSet) {
		this.author = (contentInfoSet.author==null)?"":contentInfoSet.author;
		this.description = (contentInfoSet.description==null)?"":contentInfoSet.description;
		this.publisher = (contentInfoSet.publisher==null)?"":contentInfoSet.publisher;
		this.title = (contentInfoSet.title==null)?"":contentInfoSet.title;
	}

	public String getAuthor() {
		return author;
	}
	
	public String getDescription() {
		return description;
	}
	
	public String getPublisher() {
		return publisher;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setAuthor(String author) {
		this.author = author;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	
}
