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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import de.catma.document.source.ContentInfoSet;
import de.catma.project.Project;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.util.Pair;

/**
 * A manager that handles a list of {@link AnnotationCollection}s. Handles all
 * kinds of operations upon a {@link AnnotationCollection} and its content.
 * 
 * @author marco.petris@web.de
 *
 */
public class AnnotationCollectionManager implements Iterable<AnnotationCollection>{

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private Project project;
			
	private Map<String, AnnotationCollection> collectionById;

	/**
	 * @param repository the underlying repository (addition and removal of content 
	 * of a UserMarkupCollection is passed through up to the repository 
	 */
	public AnnotationCollectionManager(Project repository) {
		this.project = repository;
		collectionById = new HashMap<String, AnnotationCollection>();
	}

	public void add(AnnotationCollection userMarkupCollection) {
		if (!this.collectionById.containsValue(userMarkupCollection)) {
			logger.info(
					"Adding UMC " + userMarkupCollection + "#" 
					+ ((userMarkupCollection == null)?"N/A":userMarkupCollection.getId())
					+ " to UserMarkupCollectionManager " + this.hashCode());
			this.collectionById.put(userMarkupCollection.getId(), userMarkupCollection);
		}
	}
	
	@Override
	public Iterator<AnnotationCollection> iterator() {
		return collectionById.values().iterator();
	}

	public void addTagReferences(
			List<TagReference> tagReferences,
			String collectionId) {
		addTagReferences(tagReferences, collectionById.get(collectionId));
	}
	
	/**
	 * Updates the UserMarkupCollection. Persistence part is handled by {@link 
	 * Project#update(AnnotationCollection, List)}
	 * @param tagReferences
	 * @param userMarkupCollection
	 */
	public void addTagReferences(
			List<TagReference> tagReferences,
			AnnotationCollection userMarkupCollection) {
	
		userMarkupCollection.addTagReferences(tagReferences);
		
		project.update(userMarkupCollection, tagReferences);

	}


	/**
	 * @return non modifiable list of the contained UserMarkupCollections
	 */
	public List<AnnotationCollection> getUserMarkupCollections() {
		return Collections.unmodifiableList(new ArrayList<>(collectionById.values()));
	}

	/**
	 * Removes the given {@link TagInstance}s from the {@link AnnotationCollection}s
	 * that contains them. If there is no such collection in this manager, this is 
	 * noop.
	 * @param instanceID the TagInstance to be removed
	 */
	public void removeTagInstance(Collection<String> instanceIDs, boolean removeFromRepo) {
		Map<AnnotationCollection, List<TagReference>> toBeDeletedByUmc = 
				new HashMap<AnnotationCollection, List<TagReference>>();
		
		for (String instanceID : instanceIDs) {
			AnnotationCollection userMarkupCollection = 
					getUserMarkupCollectionForTagInstance(instanceID); 
			// the instance may be deleted alred but can still be in a stale Analyzer-Window
			// so we silently assume that the instance has been deleted already if the
			// umc can not be found
			if (userMarkupCollection != null) {
				List<TagReference> toBeDeletedRefs = toBeDeletedByUmc.get(userMarkupCollection);
				if (toBeDeletedRefs == null) {
					toBeDeletedRefs = new ArrayList<TagReference>();
					toBeDeletedByUmc.put(userMarkupCollection, toBeDeletedRefs);
				}
				List<TagReference> tagReferences = 
						userMarkupCollection.getTagReferences(instanceID);
				userMarkupCollection.removeTagReferences(tagReferences);
				toBeDeletedRefs.addAll(tagReferences);
			}
		}
		if (removeFromRepo) {
			for (Map.Entry<AnnotationCollection, List<TagReference>> entry : toBeDeletedByUmc.entrySet()) {
				if (!entry.getValue().isEmpty()) {
					project.update(entry.getKey(), entry.getValue());
				}
			}
		}
	}
	
	/**
	 * Removes the given {@link TagInstance} from the {@link AnnotationCollection}
	 * that contains it. If there is no such collection in this manager, this is 
	 * noop.
	 * @param instanceID the TagInstance to be removed
	 */
	public void removeTagInstance(String instanceID) {
		removeTagInstance(Collections.singletonList(instanceID), true);
	}

	/**
	 * @param instanceID 
	 * @return the collections that contains the {@link TagInstance} with the
	 * given ID or <code>null</code> if there is no such collection in this
	 * manager
	 */
	private AnnotationCollection getUserMarkupCollectionForTagInstance(
			String instanceID) {
		for (AnnotationCollection userMarkupCollection : collectionById.values()) {
			if (userMarkupCollection.hasTagInstance(instanceID)) {
				return userMarkupCollection;
			}
		}
		return null;
	}

	/**
	 * @param userMarkupCollectionReference
	 * @return the referenced collection or <code>null</code> if there is
	 * no such collection in this manager
	 */
	public AnnotationCollection getUserMarkupCollection(
			AnnotationCollectionReference userMarkupCollectionReference) {
		
		return collectionById.get(userMarkupCollectionReference.getId());
	}

	public void remove(AnnotationCollection userMarkupCollection) {
		logger.info(
				"Removing UMC " + userMarkupCollection + "#" 
				+ ((userMarkupCollection == null)?"N/A":userMarkupCollection.getId())
				+ " from UserMarkupCollectionManager " + this.hashCode());
		collectionById.remove(userMarkupCollection.getId());
	}

	/**
	 * Updates {@link ContentInfoSet bibliographical Metadata} of the 
	 * given {@link AnnotationCollection}.
	 * @param userMarkupCollectionReference
	 * @return the modified collection
	 */
	public AnnotationCollection updateUserMarkupCollection(
			AnnotationCollectionReference userMarkupCollectionReference) {

		AnnotationCollection userMarkupCollection = 
				getUserMarkupCollection(userMarkupCollectionReference);
		
		userMarkupCollection.setContentInfoSet(
				userMarkupCollectionReference.getContentInfoSet());
		
		return userMarkupCollection;
	}

	/**
	 * @param instanceIDs a list of {@link TagInstance#getUuid() uuid}s of TagInstances
	 * @return a list of all TagInstances as {@link Pair pairs} with the {@link de.catma.tag.TagLibrary#getTagPath(de.catma.tag.TagDefinition) Tag path} 
	 * and the corresponding {@link TagInstance}.  
	 */
	public List<Annotation> getTagInstanceInfos(Collection<String> instanceIDs) {
		List<Annotation> result = 
				new ArrayList<Annotation>();
		for (String instanceID : instanceIDs) {
			Annotation ti = getAnnotation(instanceID);
			if (ti == null) {
				 throw new IllegalStateException(
					 "TagInstance #"+instanceID + 
					 " could not be found in this UserMarkupCollectionManager!");
			}
			result.add(ti);
		}
		return result;
	}


	public Annotation getAnnotation(String instanceID) {
		for (AnnotationCollection umc : collectionById.values()) {
			if (umc.hasTagInstance(instanceID)) {
				return umc.getAnnotation(instanceID);
			}
		}
		return null;
	}
	
	public Collection<Annotation> getAnnotations(Collection<String> tagInstanceIds) {
		return tagInstanceIds
		.stream()
		.map(tagInstanceId -> getAnnotation(tagInstanceId))
		.filter(anno -> anno != null)
		.collect(Collectors.toList());
	}

	/**
	 * @param tagInstanceID the {@link TagInstance#getUuid() uuid} of the TagInstance
	 * @return a list of the tag references of the given instance
	 */
	public Collection<TagReference> getTagReferences(String tagInstanceID) {
		Set<TagReference> result = new HashSet<TagReference>();
		for (AnnotationCollection umc : collectionById.values()) {
			if (umc.hasTagInstance(tagInstanceID)) {
				result.addAll(umc.getTagReferences(tagInstanceID));
			}
		}
		return result;
	}

	/**
	 * @param umcRef
	 * @return <code>true</code> if this manager contains the given collection (tested by
	 * id equality)
	 */
	public boolean contains(AnnotationCollectionReference umcRef) {
		return getUserMarkupCollection(umcRef) != null;
	}

	/**
	 * @param userMarkupCollectionId
	 * @return <code>true</code> if this manager contains the given collection
	 */
	public boolean contains(String userMarkupCollectionId) {
		return collectionById.containsKey(userMarkupCollectionId);
	}

	/**
	 * @param userMarkupCollectionId
	 * @return the collection or <code>null</code> if there is no such
	 * collection
	 */
	public AnnotationCollection getUserMarkupCollection(
			String userMarkupCollectionId) {
		return collectionById.get(userMarkupCollectionId);
	}

	/**
	 * The persistent part of this operation is handled by {@link
	 * Project#update(TagInstance, Collection)}
	 * @param userMarkupCollection 
	 * @param tagInstance
	 * @param properties
	 * @throws IOException
	 */
	public void updateProperty(AnnotationCollection userMarkupCollection, TagInstance tagInstance, Collection<Property> properties) throws IOException {
		project.update(userMarkupCollection, tagInstance, properties);
	}

	public void remove(String collectionId) {
		collectionById.remove(collectionId);
	}

	public void clear() {
		collectionById.clear();
	}

	public Collection<AnnotationCollectionReference> getCollections(TagDefinition tag) {
		return collectionById.values()
			.stream()
			.filter(collection -> collection.containsTag(tag))
			.map(collection -> new AnnotationCollectionReference(
				collection.getUuid(), collection.getRevisionHash(), 
				collection.getContentInfoSet(),
				collection.getSourceDocumentId(),
				collection.getSourceDocumentRevisionHash()))
			.collect(Collectors.toSet());
			
	}
}
