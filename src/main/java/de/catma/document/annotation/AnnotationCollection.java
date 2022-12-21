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
package de.catma.document.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import de.catma.document.source.ContentInfoSet;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagsetDefinition;

/**
 * A collection of user generated markup in the form of {@link TagReference}s.
 * 
 * @author marco.petris@web.de
 *
 */
public class AnnotationCollection {

	private final String uuid;
	private ContentInfoSet contentInfoSet;
	private TagLibrary tagLibrary;
	private ArrayListMultimap<String, TagReference> tagReferencesByInstanceId;
	private ArrayListMultimap<String, TagReference> tagReferencesByTagId;
	private String sourceDocumentId;
	private String forkedFromCommitURL;
	private String responsibleUser;
	private transient boolean contribution = false;
	
	/**
	 * @param id the identifier of the collections (depends on the repository)
	 * @param uuid the CATMA uuid, see {@link de.catma.util.IDGenerator}
	 * @param contentInfoSet the bibliographical metadata of this collection
	 * @param tagLibrary the internal library with all relevant {@link TagsetDefinition}s.
	 */
	public AnnotationCollection(
			String uuid, ContentInfoSet contentInfoSet, TagLibrary tagLibrary, 
			String sourceDocumentId, String forkedFromCommitURL,
			String responsibleUser) {
		this(uuid ,contentInfoSet, tagLibrary, new ArrayList<TagReference>(), 
				sourceDocumentId, forkedFromCommitURL, responsibleUser);
	}
	
	public AnnotationCollection(
			String uuid, ContentInfoSet contentInfoSet, TagLibrary tagLibrary, List<TagReference> tagReferences,
			String sourceDocumentId, String forkedFromCommitURL,
			String responsibleUser) {
		this.uuid = uuid;
		this.contentInfoSet = contentInfoSet;
		this.tagLibrary = tagLibrary;
		this.tagReferencesByInstanceId = ArrayListMultimap.create();
		this.tagReferencesByTagId = ArrayListMultimap.create();
		this.addTagReferences(tagReferences);
		this.sourceDocumentId = sourceDocumentId;
		this.forkedFromCommitURL = forkedFromCommitURL;
		this.responsibleUser = responsibleUser;
	}

	/**
	 * @return the internal library with all relevant {@link TagsetDefinition}s.
	 */
	public TagLibrary getTagLibrary() {
		return tagLibrary;
	}
	
	/**
	 * @return unmodifiable version of referenced text ranges and referencing {@link TagInstance}s.
	 */
	public List<TagReference> getTagReferences() {
		return Collections.unmodifiableList(new ArrayList<>(tagReferencesByInstanceId.values()));
	}

	/**
	 * @param tagDefinition return all references with this {@link TagDefinition}
	 *                      (<strong>excluding</strong> references that have a tag definition that is a child of the given definition,
	 *                      i.e. this is not a deep list of references)
	 * 
	 * @return the matching references
	 */
	public List<TagReference> getTagReferences(TagDefinition tagDefinition) {
		return getTagReferences(tagDefinition, false);
	}
	
	/**
	 * @param tagDefinition return all references with this tagdefinition 
	 * @param withChildReferences <code>true</code> include child tag definitions as well (deep list) or <code>false</code> exclude
	 * child tag definitions (shallow list)
	 * @return a list of tag references
	 */
	public List<TagReference> getTagReferences(
			TagDefinition tagDefinition, boolean withChildReferences) {
		
		List<TagReference> result = new ArrayList<TagReference>();
		
		Set<String> tagDefinitionIDs = new HashSet<String>();
		tagDefinitionIDs.add(tagDefinition.getUuid());
		
		if (withChildReferences) {
			tagDefinitionIDs.addAll(getChildIDs(tagDefinition));
		}
		
		for (String tagDefinitionID : tagDefinitionIDs) {
			result.addAll(tagReferencesByTagId.get(tagDefinitionID));
		}
		
		return result;
	}
	
	/**
	 * @param tagDefinition 
	 * @return a set of {@link TagDefinition#getUuid() IDs} of tag definitions that
	 * are children of the given definition (deep list)
	 */
	public Set<String> getChildIDs(TagDefinition tagDefinition) {
		return tagLibrary.getChildIDs(tagDefinition);
	}

	/**
	 * @param tagDefinition
	 * @return a set of tag definitions that are children of the given 
	 * definition (deep list)
	 */
	public List<TagDefinition> getChildren(TagDefinition tagDefinition) {
		return tagLibrary.getChildren(tagDefinition);
	}

	@Override
	public String toString() {
		return contentInfoSet.getTitle();
	}
	
	public void addTagReferences(List<TagReference> tagReferences) {
		tagReferences.forEach(tr -> {
			tagReferencesByInstanceId.put(tr.getTagInstanceId(), tr);
			tagReferencesByTagId.put(tr.getTagDefinitionId(), tr);
		});
	}
	
	public void addTagReference(TagReference tagReference) {
		this.addTagReferences(Collections.singletonList(tagReference));
	}
	
	/**
	 * @return the identifier of this collection (depends on the repository) 
	 */
	public String getId() {
		return this.uuid;
	}

	/**
	 * @return the CATMA uuid of this collection
	 */
	
	public String getUuid() {
		return this.uuid;
	}
	
	/**
	 * @return name of this collection
	 */
	public String getName() {
		return contentInfoSet.getTitle();
	}
	
	/**
	 * @return metadata for this collection
	 */
	public ContentInfoSet getContentInfoSet() {
		return contentInfoSet;
	}
	
	/**
	 * @return <code>true</code> if there are no tag references in this collection
	 */
	public boolean isEmpty() {
		return tagReferencesByInstanceId.isEmpty();
	}
	
	/**
	 * @param tagLibrary the internal library with all relevant {@link TagsetDefinition}s
	 * must correspond to the tag definitions of the tag intances of the tag references 
	 * of this collection
	 */
	public void setTagLibrary(TagLibrary tagLibrary) {
		this.tagLibrary = tagLibrary;
	}


	public String getForkedFromCommitURL() {
		return forkedFromCommitURL;
	}
	
	public void setForkedFromCommitURL(String forkedFromCommitURL) {
		this.forkedFromCommitURL = forkedFromCommitURL;
	}
	
	public String getResponsibleUser() {
		return responsibleUser;
	}
	
	public void setResponsibleUser(String responsibleUser) {
		this.responsibleUser = responsibleUser;
	}
	
	/**
	 * @param tagInstanceID
	 * @return all references which belong to the {@link TagInstance} with the given ID
	 */
	public List<TagReference> getTagReferences(String tagInstanceID) {
		List<TagReference> result = new ArrayList<TagReference>();
		result.addAll(tagReferencesByInstanceId.get(tagInstanceID));
		
		return result;
	}

	/**
	 * @param ti
	 * @return all references which belong to the given TagInstance
	 */
	public List<TagReference> getTagReferences(TagInstance ti) {
		return getTagReferences(ti.getUuid());
	}
	
	/**
	 * @param instanceID
	 * @return <code>true</code> if there is a TagReference with the given TagInstance's ID
	 */
	public boolean hasTagInstance(String instanceID) {
		return tagReferencesByInstanceId.containsKey(instanceID);
	}
	
	/**
	 * @param tagReferences references to be removed
	 */
	public void removeTagReferences(List<TagReference> tagReferences) {
		for (TagReference tagReference : tagReferences) {
			tagReferencesByInstanceId.remove(tagReference.getTagInstanceId(), tagReference);
			tagReferencesByTagId.remove(tagReference.getTagDefinitionId(), tagReference);
		}
	}


	/**
	 * @param tagsetDefinition
	 * @return all references that have TagInstances with the given tag def
	 */
	public List<TagReference> getTagReferences(TagsetDefinition tagsetDefinition) {
		ArrayList<TagReference> result = new ArrayList<TagReference>();
		if (getTagLibrary().contains(tagsetDefinition)) {
			for (TagDefinition td : getTagLibrary().getTagsetDefinition(
					tagsetDefinition.getUuid())) {
				
				result.addAll(getTagReferences(td));
				
			}
		}
		
		return result;
	}
	
	/**
	 * @param contentInfoSet Metadata for this collection
	 */
	void setContentInfoSet(ContentInfoSet contentInfoSet) {
		this.contentInfoSet = contentInfoSet;
	}

	public Annotation getAnnotation(String tagInstanceId) {
		List<TagReference> tagReferences = getTagReferences(tagInstanceId);
		
		TagInstance tagInstance = tagReferences.get(0).getTagInstance();
		String tagPath = getTagLibrary().getTagPath(tagInstance.getTagDefinitionId());
		
		return new Annotation(tagInstance, tagReferences, this, tagPath);
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
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		AnnotationCollection other = (AnnotationCollection) obj;
		if (uuid == null) {
			if (other.uuid != null) {
				return false;
			}
		} else if (!uuid.equals(other.uuid)) {
			return false;
		}
		return true;
	}

	public boolean containsTag(TagDefinition tag) {
		return this.tagReferencesByTagId.containsKey(tag.getUuid());
	}

	public String getSourceDocumentId() {
		return sourceDocumentId;
	}

	public boolean isResponsible(String userIdentifier) {
		if (responsibleUser != null) {
			return responsibleUser.equals(userIdentifier);
		}
		return true; // shared responsibility
	}

	public Multimap<String, TagReference> getTagReferencesByInstanceId(TagDefinition tag) {
		Multimap<String, TagReference> tagReferencesByInstanceId = ArrayListMultimap.create();
		
		getTagReferences(tag).stream()
			.forEach(tr -> tagReferencesByInstanceId.put(tr.getTagInstanceId(), tr));
		
		return tagReferencesByInstanceId;

	}

	public void mergeAdditive(AnnotationCollection collection) {
		
		Set<TagInstance> toBeMerged = new HashSet<TagInstance>();
		for (TagReference reference : collection.getTagReferences()) {
			if (!hasTagInstance(reference.getTagInstanceId())) {
				addTagReference(reference);
				setContribution(true);
			}
			else {
				toBeMerged.add(reference.getTagInstance());
			}
		}
		
		for (TagInstance tagInstance : toBeMerged) {
			boolean merged = 
				tagReferencesByInstanceId
				.get(tagInstance.getUuid())
				.get(0)
				.getTagInstance()
				.mergeAdditive(tagInstance);
			if (merged) {
				setContribution(true);
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
