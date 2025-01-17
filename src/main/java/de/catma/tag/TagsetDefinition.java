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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A set of {@link TagDefinition}s.
 * 
 * @author marco.petris@web.de
 *
 */
public class TagsetDefinition implements Iterable<TagDefinition> {
	
	private String uuid;
	private String name;

	private String description;
	private String forkedFromCommitURL;
	private String responsibleUser;
	
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Map<String,TagDefinition> tagDefinitions;
	private Map<String,Set<String>> tagDefinitionChildren;
	private Set<String> deletedDefinitions;
	private transient boolean contribution = false;

	/**
	 * @param id a repository dependent identifier
	 * @param uuid the CATMA uuid, see {@link de.catma.util.IDGenerator}
	 * @param tagsetName the name of the tagset
	 * @param version the version of the tagset
	 */
	public TagsetDefinition(
			String uuid, String tagsetName) {
		this(uuid, tagsetName, new HashSet<>());
	}
	
	public TagsetDefinition(
			String uuid, String tagsetName,
			Set<String> deletedDefinitions) {
		this.tagDefinitions = new HashMap<String, TagDefinition>();
		this.tagDefinitionChildren = new HashMap<String, Set<String>>();
		this.deletedDefinitions = deletedDefinitions;
		this.uuid = uuid;
		this.name = tagsetName;
	}

	/**
	 * Copy constructor
	 * @param toCopy
	 */
	public TagsetDefinition(TagsetDefinition toCopy) {
		this (toCopy.uuid, toCopy.name);
		for (TagDefinition tagDefinition : toCopy) {
			addTagDefinition(new TagDefinition(tagDefinition));
		}
	}
	
	@Override
	public String toString() {
		return "TAGSET_DEF["+name+",#"+uuid+"]";
	}

	public void addTagDefinition(TagDefinition tagDef) {
		tagDefinitions.put(tagDef.getUuid(),tagDef);
		if (!tagDefinitionChildren.containsKey(tagDef.getParentUuid())) {
			tagDefinitionChildren.put(
					tagDef.getParentUuid(), new HashSet<String>());
		}
		tagDefinitionChildren.get(
				tagDef.getParentUuid()).add(tagDef.getUuid());
		
		deletedDefinitions.remove(tagDef.getUuid());
	}
	
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) { this.uuid = uuid; }
	
	/**
	 * @param tagDefID CATMA uuid of the {@link TagDefinition}, see {@link de.catma.util.IDGenerator}
	 * @return <code>true</code> if this tagset def contains the corresponding tag def
	 */
	public boolean hasTagDefinition(String tagDefID) {
		return tagDefinitions.containsKey(tagDefID);
	}
	
	/**
	 * @param tagDefinitionID CATMA uuid of the {@link TagDefinition}, see {@link de.catma.util.IDGenerator}
	 * @return the corresponding TagDefinition or <code>null</code> if there is no such definition in this
	 * tagset def
	 */
	public TagDefinition getTagDefinition(String tagDefinitionID) {
		return tagDefinitions.get(tagDefinitionID);
	}
	
	public Stream<TagDefinition> getTagDefinitionsByName(String name) {
		return tagDefinitions.values().stream().filter(tag -> tag.getName().equals(name));
	}
	
	public Iterator<TagDefinition> iterator() {
		return this.stream().iterator();
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean contains(TagDefinition tagDefinition) {
		return tagDefinitions.values().contains(tagDefinition);
	}
	
	public List<TagDefinition> getRootTagDefinitions() {
		return tagDefinitions.values()
		.stream()
		.filter(tagDef -> tagDef.getParentUuid().isEmpty())
		.sorted(new Comparator<TagDefinition>() {
			@Override
			public int compare(TagDefinition o1, TagDefinition o2) { //TODO: sort by predefined sort order
				if (o1.getName().equals(o2.getName())) {
					return o1.getUuid().compareTo(o2.getUuid());
				}
				return o1.getName().compareTo(o2.getName());
			}
		})
		.collect(Collectors.toList());
	}

	public List<TagDefinition> getDirectChildren(TagDefinition tagDefinition) {
		List<TagDefinition> children = new ArrayList<TagDefinition>();
		Set<String> directChildrenIDs = 
				tagDefinitionChildren.get(tagDefinition.getUuid());
		
		if (directChildrenIDs == null) {
			return Collections.emptyList();
			
		}
		
		for (String childID : directChildrenIDs) {
			TagDefinition child = getTagDefinition(childID); 
			children.add(child);
		}
		
		return Collections.unmodifiableList(children);
	}
	
	/**
	 * @param tagDefinition
	 * @return an unmodifiable list of all child TagDefinitions of the given
	 * TagDefinition (deep list)
	 */
	public List<TagDefinition> getChildren(TagDefinition tagDefinition) {
		List<TagDefinition> children = new ArrayList<TagDefinition>();
		Set<String> directChildrenIDs = 
				tagDefinitionChildren.get(tagDefinition.getUuid());
		
		if (directChildrenIDs == null) {
			return Collections.emptyList();
			
		}
		
		for (String childID : directChildrenIDs) {
			TagDefinition child = getTagDefinition(childID); 
			children.add(child);
			children.addAll(getChildren(child));
		}

		return Collections.unmodifiableList(children);
	}

	/**
	 * @param tagDefinition
	 * @return a set of the uuids of the child TagDefinitions of the given
	 * TagDefinition
	 */
	Set<String> getChildIDs(TagDefinition tagDefinition) {
		Set<String> childIDs = new HashSet<String>();
		Set<String> directChildrenIDs = 
				tagDefinitionChildren.get(tagDefinition.getUuid());
		
		if (directChildrenIDs == null) {
			return Collections.emptySet();
			
		}
		
		for (String childID : directChildrenIDs) {
			TagDefinition child = getTagDefinition(childID); 
			childIDs.add(child.getUuid());
			childIDs.addAll(getChildIDs(child));
		}

		return Collections.unmodifiableSet(childIDs);	
	}

	public void reParent(TagDefinition item, String parent) {
		if (!tagDefinitions.containsKey(item.getUuid())) {
			logger.warning("Can't reparent: there is no such TagDefinition in this tagset");
			return;
		}
		if (item.getParentUuid() == parent) {
			logger.warning("Parent hasn't moved, not changing anything");
			return;
		}
		if (!tagDefinitionChildren.containsKey(item.getParentUuid())) {
			logger.warning("The item parent doesn't seem to have any children");
			return;
		}
		if (!tagDefinitionChildren.get(item.getParentUuid()).contains(item.getUuid())) {
			logger.warning("The item parent doesn't contain itself");
			return;
		}
		if (!tagDefinitionChildren.containsKey(parent)) {
			tagDefinitionChildren.put(parent, new HashSet<String>());
		}
		tagDefinitionChildren.get(parent).add(item.getUuid());
		tagDefinitionChildren.get(item.getParentUuid()).remove(item.getUuid());
	}

	public void remove(TagDefinition tagDefinition) {
		for (TagDefinition child : getChildren(tagDefinition)) {
			remove(child);
		}
		this.deletedDefinitions.add(
			this.tagDefinitions.remove(tagDefinition.getUuid())
			.getUuid());
		removeFromChildrenCache(tagDefinition);
	}
	
	private void removeFromChildrenCache(TagDefinition tagDefinition) {
		Set<String> childrenOfParent = this.tagDefinitionChildren.get(
				tagDefinition.getParentUuid());
		if (childrenOfParent != null) {
			childrenOfParent.remove(tagDefinition.getUuid());
		}
		this.tagDefinitionChildren.remove(tagDefinition.getUuid());
	}

	/**
	 * @param tagDefinition
	 * @return the path from the top level TagDefinition down to the given TagDefintion
	 */
	public String getTagPath(TagDefinition tagDefinition) {
		
		StringBuilder builder = new StringBuilder();
		builder.append("/");
		builder.append(tagDefinition.getName());
		String baseID = tagDefinition.getParentUuid();
		
		while (!baseID.isEmpty()) {
			TagDefinition parentDef = getTagDefinition(baseID);
			builder.insert(0, parentDef.getName());
			builder.insert(0, "/");
			
			baseID = parentDef.getParentUuid();
		}
		
		return builder.toString();
	}
	
	public boolean isEmpty() {
		return tagDefinitions.isEmpty();
	}

	public boolean isDeleted(String definitionUuid) {
		return this.deletedDefinitions.contains(definitionUuid);
	}
	
	public Set<String> getDeletedDefinitions() {
		return Collections.unmodifiableSet(deletedDefinitions);
	}

	public void remove(PropertyDefinition propertyDefinition, TagDefinition tagDefinition) {
		tagDefinition.removeUserDefinedPropertyDefinition(propertyDefinition);
		this.deletedDefinitions.add(propertyDefinition.getUuid());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TagsetDefinition other = (TagsetDefinition) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

	public int size() {
		return tagDefinitions.size();
	}

	public Stream<TagDefinition> stream() {
		return getRootTagDefinitions().stream().flatMap(root -> root.directChildrenStream(this));
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getForkedFromCommitURL() {
		return forkedFromCommitURL;
	}
	
	public void setForkedFromCommitURL(String forkedFromCommitURL) {
		this.forkedFromCommitURL = forkedFromCommitURL;
	}
	
	public void setResponsibleUser(String responsibleUser) {
		this.responsibleUser = responsibleUser;
	}
	
	public String getResponsibleUser() {
		return responsibleUser;
	}

	public boolean isResponsible(String userIdentifier) {
		if (responsibleUser != null) {
			return responsibleUser.equals(userIdentifier);
		}
		return true; // shared responsibility
	}

	public void mergeAdditive(TagsetDefinition tagset) {
		
		for (TagDefinition tag: tagset) {
			if (hasTagDefinition(tag.getUuid())) {
				boolean tagHashContributions = getTagDefinition(tag.getUuid()).mergeAdditive(tag);
				if (tagHashContributions) {
					contribution = true;
				}
			}
			else {
				tag.setContribution(true);
				addTagDefinition(tag);
				contribution = true;
			}
		}
		
	}
	
	public boolean isContribution() {
		return contribution;
	}
	
	public void setContribution(boolean contribution) {
		this.contribution = contribution;
	}
}
