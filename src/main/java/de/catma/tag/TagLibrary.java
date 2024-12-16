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
package de.catma.tag;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

/**
 * A library of {@link TagsetDefinition}s.
 * 
 * @author marco.petris@web.de
 *
 */
public class TagLibrary implements Iterable<TagsetDefinition> {

	private Map<String,TagsetDefinition> tagsetDefinitionsByID;
	private Locale locale;
	
	public TagLibrary() {
		tagsetDefinitionsByID = new HashMap<String, TagsetDefinition>();
	}

	void add(TagsetDefinition tagsetDefinition) {
		tagsetDefinitionsByID.put(tagsetDefinition.getUuid(),tagsetDefinition);
	}

	//FIXME: this assumes that there is only one tagsetdef that can contain a
	// tagdef identified by id, this is not true in all cases for incoming tagsetdefs
	// of older CATMA versions since move operations between tagsets were possible and
	// the conversion algorithm of the standard tagset can in certain cases generate distinct IDs, 
	// supporting move in CATMA 4 would certainly break this assumption as well!!!
	
	// there is only one TagLibrary per project, so within a project, this should work fine in CATMA 6 now
	/**
	 * @param tagDefinitionID CATMA uuid of the {@link TagDefinition}, see {@link de.catma.util.IDGenerator}.
	 * @return the corresponding TagDefinition or <code>null</code>
	 */
	public TagDefinition getTagDefinition(String tagDefinitionID) {
		for(TagsetDefinition tagsetDefiniton : tagsetDefinitionsByID.values()) {
			if (tagsetDefiniton.hasTagDefinition(tagDefinitionID)) {
				return tagsetDefiniton.getTagDefinition(tagDefinitionID);
			}
		}
		return null;
	}
	
	public Iterator<TagsetDefinition> iterator() {
		return Collections.unmodifiableCollection(tagsetDefinitionsByID.values()).iterator();
	}
	
	public Collection<TagsetDefinition> getTagsetDefinitions() {
		return Collections.unmodifiableCollection(tagsetDefinitionsByID.values());
	}

	/**
	 * @param tagsetDefinitionID CATMA uuid of the {@link TagsetDefinition}, see {@link de.catma.util.IDGenerator}.
	 * @return  the corresponding TagsetDefinition or <code>null</code>
	 */
	public TagsetDefinition getTagsetDefinition(String tagsetDefinitionID) {
		return tagsetDefinitionsByID.get(tagsetDefinitionID);
	}

	/**
	 * @param tagDefinition
	 * @return all child definitions of the given tag definition 
	 */
	public List<TagDefinition> getChildren(TagDefinition tagDefinition) {
		TagsetDefinition tagsetDefinition = getTagsetDefinition(tagDefinition);
		return tagsetDefinition.getChildren(tagDefinition);
	}

	/**
	 * @param tagDefinition
	 * @return the TagsetDefinition for the given TagDefinition or <code>null</code>.
	 */
	public TagsetDefinition getTagsetDefinition(TagDefinition tagDefinition) {
		for (TagsetDefinition td : this) {
			if (td.contains(tagDefinition)) {
				return td;
			}
		}
		return null;
	}

	/**
	 * @param tagDefinition
	 * @return a set of CATMA uuids of tag definition children of the given
	 * TagDefinition
	 */
	public Set<String> getChildIDs(TagDefinition tagDefinition) {
		TagsetDefinition tagsetDefinition = getTagsetDefinition(tagDefinition);
		return tagsetDefinition.getChildIDs(tagDefinition);
	}

	public void remove(TagsetDefinition tagsetDefinition) {
		tagsetDefinitionsByID.remove(tagsetDefinition.getUuid());
	}

	/**
	 * @param tagsetDefinition the tagsetDefinition is tested by {@link TagsetDefinition#getUuid()} only,
	 * so even if this TagLibrary contains another instance with the same uuid this method
	 * will return <code>true</code>! 
	 * @return true, if this TagLibrary contains a TagsetDefinition that has
	 * uuid equality with the given tagsetDefinition, else false.
	 */
	public boolean contains(TagsetDefinition tagsetDefinition) {
		return tagsetDefinitionsByID.containsKey(tagsetDefinition.getUuid());
	}
	
	/**
	 * @param tagDefinition
	 * @return the path from the top level TagDefinition down to the given TagDefinition 
	 */
	public String getTagPath(TagDefinition tagDefinition) {
		TagsetDefinition tagsetDefinition = getTagsetDefinition(tagDefinition);
		return tagsetDefinition.getTagPath(tagDefinition);
	}

	public String getTagPath(String tagId) {
		TagDefinition tagDefinition = getTagDefinition(tagId);
		TagsetDefinition tagsetDefinition = getTagsetDefinition(tagDefinition);
		return tagsetDefinition.getTagPath(tagDefinition);
	}

	void clear() {
		tagsetDefinitionsByID.clear();
	}
	
	public boolean isDeleted(String definitionUuid) {
		for (TagsetDefinition tagset : this) {
			if (tagset.isDeleted(definitionUuid)) {
				return true;
			}
		}
		
		return false;
	}
	
	public Locale getLocale() {
		if (locale == null) {
			this.locale = tagsetDefinitionsByID.values().stream().map(TagsetDefinition::getName).reduce((s1, s2) -> String.join(",", s1, s2)).map(names -> {
				LanguageDetector languageDetector = LanguageDetector.getDefaultLanguageDetector();
				try {
					languageDetector.loadModels();
					LanguageResult languageResult = languageDetector.detect(names);
					return Locale.forLanguageTag(languageResult.getLanguage());
				}
				catch (IOException e) {
					return Locale.getDefault();
				}
			}).orElse(Locale.getDefault());
		}
		
		return this.locale;
	}
}
