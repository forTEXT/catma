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
 * Metadata for a {@link SourceDocument}.
 * 
 * @author marco.petris@web.de
 *
 */
public class SourceDocumentInfo {

	private IndexInfoSet indexInfoSet;
	private ContentInfoSet contentInfoSet;
	private TechInfoSet techInfoSet;
	
	public SourceDocumentInfo() {
	}
	
	/**
	 * @param indexInfoSet index metadata
	 * @param contentInfoSet bibliographical info
	 * @param techInfoSet technical metadata about the physical source
	 */
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
	
	public void setTechInfoSet(TechInfoSet techInfoSet) {
		this.techInfoSet = techInfoSet;
	}
	
	public void setContentInfoSet(ContentInfoSet contentInfoSet) {
		this.contentInfoSet = contentInfoSet;
	}
	
	public void setIndexInfoSet(IndexInfoSet indexInfoSet) {
		this.indexInfoSet = indexInfoSet;
	}
}
