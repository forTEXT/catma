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
package de.catma.ui.tagger;

import de.catma.tag.TagInstance;

/**
 * Display information for the selected {@link TagInstance}.
 * 
 * @author alexandra.krah@googlemail.com
 *
 */
public class TagInstanceInfoSet {
	
	private String collection;
	private String path;
	private String ID;
	private TagInstance instance;
	

	public TagInstanceInfoSet(String collection, String path,
			String ID, TagInstance instance) {
		super();
		this.collection = collection;
		this.path = path;
		this.ID = ID;
		this.instance = instance;
	}

	/**
	 * Empty {@link TagInstance} information.
	 */
	public TagInstanceInfoSet(){
		this.collection = "";
		this.path = "";
		this.ID = "";
	}
	
	
	public String getCollection(){
		return collection;
	}
	
	public String getPath(){
		return path;
	}
	
	public String getID(){
		return ID;
	}
	
	public String getInstance(){
		return instance==null?"":instance.getTagDefinition().getName();
	}

	
}
