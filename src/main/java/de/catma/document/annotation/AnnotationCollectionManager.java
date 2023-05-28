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

import de.catma.project.Project;
import de.catma.tag.Property;
import de.catma.tag.TagDefinition;
import de.catma.tag.TagInstance;

import java.util.*;
import java.util.stream.Collectors;

public class AnnotationCollectionManager implements Iterable<AnnotationCollection>{
	private final Project project;

	private final Map<String, AnnotationCollection> collectionsById;

	/**
	 * @param project the underlying {@link Project} (addition, modification and removal of annotations
	 *                in an {@link AnnotationCollection} is passed through to the project)
	 */
	public AnnotationCollectionManager(Project project) {
		this.project = project;

		collectionsById = new HashMap<>();
	}

	@Override
	public Iterator<AnnotationCollection> iterator() {
		return collectionsById.values().iterator();
	}

	public boolean contains(String annotationCollectionId) {
		return collectionsById.containsKey(annotationCollectionId);
	}

	public void clear() {
		collectionsById.clear();
	}

	/**
	 * @return an unmodifiable list of the contained {@link AnnotationCollection}s
	 */
	public List<AnnotationCollection> getAnnotationCollections() {
		return Collections.unmodifiableList(new ArrayList<>(collectionsById.values()));
	}

	public AnnotationCollection getAnnotationCollection(String annotationCollectionId) {
		return collectionsById.get(annotationCollectionId);
	}

	private AnnotationCollection getAnnotationCollectionForTagInstance(String tagInstanceId) {
		for (AnnotationCollection annotationCollection : collectionsById.values()) {
			if (annotationCollection.hasTagInstance(tagInstanceId)) {
				return annotationCollection;
			}
		}
		return null;
	}

	public Collection<AnnotationCollectionReference> getCollectionReferencesForTagDefinition(TagDefinition tagDefinition) {
		return collectionsById.values()
				.stream()
				.filter(collection -> collection.containsTag(tagDefinition))
				.map(collection -> new AnnotationCollectionReference(
						collection.getUuid(),
						collection.getContentInfoSet(),
						collection.getSourceDocumentId(),
						collection.getForkedFromCommitURL(),
						collection.getResponsibleUser()
				))
				.collect(Collectors.toSet());
	}

	public Annotation getAnnotationForTagInstance(String tagInstanceId) {
		for (AnnotationCollection annotationCollection : collectionsById.values()) {
			if (annotationCollection.hasTagInstance(tagInstanceId)) {
				return annotationCollection.getAnnotation(tagInstanceId);
			}
		}
		return null;
	}

	public Collection<Annotation> getAnnotationsForTagInstances(Collection<String> tagInstanceIds) {
		return tagInstanceIds.stream()
				.map(this::getAnnotationForTagInstance)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public void add(AnnotationCollection annotationCollection) {
		if (collectionsById.containsKey(annotationCollection.getUuid())) {
			return;
		}

		collectionsById.put(annotationCollection.getUuid(), annotationCollection);
	}

	public void remove(String annotationCollectionId) {
		collectionsById.remove(annotationCollectionId);
	}

	public void addTagReferences(List<TagReference> tagReferences, AnnotationCollection annotationCollection) {
		annotationCollection.addTagReferences(tagReferences);
		project.addTagReferencesToCollection(annotationCollection, tagReferences);
	}

	public void addTagReferences(List<TagReference> tagReferences, String annotationCollectionId) {
		addTagReferences(tagReferences, collectionsById.get(annotationCollectionId));
	}

	public void updateTagInstanceProperties(AnnotationCollection annotationCollection, TagInstance tagInstance, Collection<Property> properties) {
		project.updateTagInstanceProperties(annotationCollection, tagInstance, properties);
	}

	/**
	 * Removes the given {@link TagInstance}s from the {@link AnnotationCollection}s that contain them.
	 * <p>
	 * This is a no-op if this manager does not contain the corresponding collection(s).
	 *
	 * @param tagInstanceIds IDs of the tag instances to be removed
	 * @param removeFromRepo whether the tag instances should be removed from persistent storage as well
	 */
	public void removeTagInstances(Collection<String> tagInstanceIds, boolean removeFromRepo) {
		Map<AnnotationCollection, List<TagReference>> tagReferencesToBeDeletedByAnnotationCollection = new HashMap<>();

		for (String tagInstanceId : tagInstanceIds) {
			AnnotationCollection annotationCollection = getAnnotationCollectionForTagInstance(tagInstanceId);

			// the tag instance may be deleted already but can still be present in a stale 'Analyze' window/tab
			// we silently assume that it has been deleted already if the corresponding collection cannot be found
			if (annotationCollection == null) {
				continue;
			}

			List<TagReference> tagReferencesForTagInstance = annotationCollection.getTagReferences(tagInstanceId);

			if (!tagReferencesToBeDeletedByAnnotationCollection.containsKey(annotationCollection)) {
				tagReferencesToBeDeletedByAnnotationCollection.put(annotationCollection, tagReferencesForTagInstance);
			}
			else {
				tagReferencesToBeDeletedByAnnotationCollection.get(annotationCollection).addAll(tagReferencesForTagInstance);
			}

			annotationCollection.removeTagReferences(tagReferencesForTagInstance);
		}

		if (removeFromRepo) {
			for (Map.Entry<AnnotationCollection, List<TagReference>> entry : tagReferencesToBeDeletedByAnnotationCollection.entrySet()) {
				project.removeTagReferencesFromCollection(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Removes the {@link TagInstance} identified by the given ID from the {@link AnnotationCollection} that contains it.
	 * <p>
	 * NB: The tag instance will be removed from persistent storage as well!
	 * This is a no-op if this manager does not contain the corresponding collection.
	 *
	 * @param tagInstanceId the ID of the tag instance to be removed
	 */
	public void removeTagInstance(String tagInstanceId) {
		removeTagInstances(Collections.singletonList(tagInstanceId), true);
	}
}
