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
package de.catma.document.standoffmarkup.usermarkup;

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

import de.catma.document.AccessMode;
import de.catma.document.repository.Repository;
import de.catma.document.source.ContentInfoSet;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.util.Pair;

/**
 * A manager that handles a list of {@link UserMarkupCollection}s. Handles all
 * kinds of operations upon a {@link UserMarkupCollection} and its content.
 * 
 * @author marco.petris@web.de
 *
 */
public class UserMarkupCollectionManager implements Iterable<UserMarkupCollection>{

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private TagManager tagManager;
	private Repository repository;
			
	private List<UserMarkupCollection> userMarkupCollections;

	/**
	 * @param repository the underlying repository (addition and removal of content 
	 * of a UserMarkupCollection is passed through up to the repository 
	 */
	public UserMarkupCollectionManager(Repository repository) {
		this.tagManager = repository.getTagManager();
		this.repository = repository;
		userMarkupCollections = new ArrayList<UserMarkupCollection>();
	}
	
	/**
	 * Updates given UserMarkupCollections
	 * with the TagsetDefinition. That is all the {@link de.catma.tag.TagLibrary TagLibraries}
	 * and all the {@link TagInstance}s are updated with the new TagsetDefinition.
	 * The actual persistent modifications are made through {@link Repository#update(List, TagsetDefinition)}.
	 * @param outOfSynchCollections
	 * @param tagsetDefinition
	 */
	@Deprecated
	public void updateUserMarkupCollections(
			List<UserMarkupCollection> outOfSynchCollections, 
			TagsetDefinition tagsetDefinition) {
		
		for (UserMarkupCollection userMarkupCollection : outOfSynchCollections) {
			logger.info("synching " + userMarkupCollection);
			userMarkupCollection.synchronizeTagInstances();
		}

		repository.update(outOfSynchCollections, tagsetDefinition);
	}
	

	public void add(UserMarkupCollection userMarkupCollection) {
		if (!this.userMarkupCollections.contains(userMarkupCollection)) {
			logger.info(
					"Adding UMC " + userMarkupCollection + "#" 
					+ ((userMarkupCollection == null)?"N/A":userMarkupCollection.getId())
					+ " to UserMarkupCollectionManager " + this.hashCode());
			this.userMarkupCollections.add(userMarkupCollection);
		}
	}
	
	@Override
	public Iterator<UserMarkupCollection> iterator() {
		return userMarkupCollections.iterator();
	}

	/**
	 * Updates the UserMarkupCollection. Persistence part is handled by {@link 
	 * Repository#update(UserMarkupCollection, List)}
	 * @param tagReferences
	 * @param userMarkupCollection
	 */
	public void addTagReferences(
			List<TagReference> tagReferences,
			UserMarkupCollection userMarkupCollection) {
	
		userMarkupCollection.addTagReferences(tagReferences);
		
		repository.update(userMarkupCollection, tagReferences);

	}


	/**
	 * @return non modifiable list of the contained UserMarkupCollections
	 */
	public List<UserMarkupCollection> getUserMarkupCollections() {
		return Collections.unmodifiableList(userMarkupCollections);
	}


	/**
	 * @param tagsetDefinition
	 * TagsetDefinition
	 * @return a list {@link TagsetDefinition#isSynchronized(TagsetDefinition) of out ot synch} UserMarkupCollections
	 */
	public List<UserMarkupCollection> getOutOfSyncUserMarkupCollections(
			TagsetDefinition tagsetDefinition) {
		
		List<UserMarkupCollection> result = 
				new ArrayList<UserMarkupCollection>();
		
//		for (UserMarkupCollection userMarkupCollection : userMarkupCollections) {
//			// FIXME: regardless of tagsetdef containment, check tagdef containment as well to support old standard tagsets and move operations
//			
//			// no need to check non writable collections, they won't be updated anyway
//			if (userMarkupCollection.getAccessMode().equals(AccessMode.WRITE) 
//					&& 
//					userMarkupCollection.getTagLibrary().contains(tagsetDefinition)) {
//				result.add(userMarkupCollection);
//			}
//			
//		}
		
		return result;
	}

	/**
	 * Removes the given {@link TagInstance}s from the {@link UserMarkupCollection}s
	 * that contains them. If there is no such collection in this manager, this is 
	 * noop.
	 * @param instanceID the TagInstance to be removed
	 */
	public void removeTagInstance(Collection<String> instanceIDs, boolean removeFromRepo) {
		Map<UserMarkupCollection, List<TagReference>> toBeDeletedByUmc = 
				new HashMap<UserMarkupCollection, List<TagReference>>();
		
		for (String instanceID : instanceIDs) {
			UserMarkupCollection userMarkupCollection = 
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
			for (Map.Entry<UserMarkupCollection, List<TagReference>> entry : toBeDeletedByUmc.entrySet()) {
				if (!entry.getValue().isEmpty()) {
					repository.update(entry.getKey(), entry.getValue());
				}
			}
		}
	}
	
	/**
	 * Removes the given {@link TagInstance} from the {@link UserMarkupCollection}
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
	private UserMarkupCollection getUserMarkupCollectionForTagInstance(
			String instanceID) {
		for (UserMarkupCollection userMarkupCollection : userMarkupCollections) {
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
	public UserMarkupCollection getUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference) {
		for (UserMarkupCollection umc : userMarkupCollections) {
			if (umc.getId().equals(userMarkupCollectionReference.getId())) {
				return umc;
			}
		}
		
		return null;
	}

	public void remove(UserMarkupCollection userMarkupCollection) {
		logger.info(
				"Removing UMC " + userMarkupCollection + "#" 
				+ ((userMarkupCollection == null)?"N/A":userMarkupCollection.getId())
				+ " from UserMarkupCollectionManager " + this.hashCode());
		userMarkupCollections.remove(userMarkupCollection);
	}

	/**
	 * Updates {@link ContentInfoSet bibliographical Metadata} of the 
	 * given {@link UserMarkupCollection}.
	 * @param userMarkupCollectionReference
	 * @return the modified collection
	 */
	public UserMarkupCollection updateUserMarkupCollection(
			UserMarkupCollectionReference userMarkupCollectionReference) {

		UserMarkupCollection userMarkupCollection = 
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
		for (UserMarkupCollection umc : userMarkupCollections) {
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
		.collect(Collectors.toList());
	}

	/**
	 * @param tagInstanceID the {@link TagInstance#getUuid() uuid} of the TagInstance
	 * @return a list of the tag references of the given instance
	 */
	public Collection<TagReference> getTagReferences(String tagInstanceID) {
		Set<TagReference> result = new HashSet<TagReference>();
		for (UserMarkupCollection umc : userMarkupCollections) {
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
	public boolean contains(UserMarkupCollectionReference umcRef) {
		return getUserMarkupCollection(umcRef) != null;
	}

	/**
	 * @param userMarkupCollectionId
	 * @return <code>true</code> if this manager contains the given collection
	 */
	public boolean contains(String userMarkupCollectionId) {
		for (UserMarkupCollection umc : userMarkupCollections) {
			if (umc.getId().equals(userMarkupCollectionId)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param userMarkupCollectionId
	 * @return the collection or <code>null</code> if there is no such
	 * collection
	 */
	public UserMarkupCollection getUserMarkupCollection(
			String userMarkupCollectionId) {
		for (UserMarkupCollection umc : userMarkupCollections) {
			if (umc.getId().equals(userMarkupCollectionId)) {
				return umc;
			}
		}
		
		return null;
	}

	/**
	 * The persistent part of this operation is handled by {@link
	 * Repository#update(TagInstance, Collection)}
	 * @param userMarkupCollection 
	 * @param tagInstance
	 * @param properties
	 * @throws IOException
	 */
	public void updateProperty(UserMarkupCollection userMarkupCollection, TagInstance tagInstance, Collection<Property> properties) throws IOException {
		repository.update(userMarkupCollection, tagInstance, properties);
	}

	public void remove(String collectionId) {
		userMarkupCollections
			.stream()
			.filter(collection -> collection.getUuid().equals(collectionId))
			.findFirst()
			.ifPresent(collection -> remove(collection));
	}

	public void clear() {
		userMarkupCollections.clear();
		
	}

	public Collection<UserMarkupCollectionReference> getCollections(TagDefinition tag) {
		return userMarkupCollections
			.stream()
			.filter(collection -> collection.containsTag(tag))
			.map(collection -> new UserMarkupCollectionReference(
				collection.getUuid(), collection.getRevisionHash(), 
				collection.getContentInfoSet(),
				collection.getSourceDocumentId(),
				collection.getSourceDocumentRevisionHash()))
			.collect(Collectors.toSet());
			
	}
}
