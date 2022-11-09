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
package de.catma.project;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.annotation.TagReference;
import de.catma.document.comment.Comment;
import de.catma.document.comment.Reply;
import de.catma.document.source.ContentInfoSet;
import de.catma.document.source.SourceDocument;
import de.catma.document.source.SourceDocumentReference;
import de.catma.rbac.RBACPermission;
import de.catma.rbac.RBACRole;
import de.catma.rbac.RBACSubject;
import de.catma.serialization.TagsetDefinitionImportStatus;
import de.catma.tag.*;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.Pair;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Conceptually, and in the persistence layer, a project is a container for {@link SourceDocument}s, {@link AnnotationCollection}s
 * and a {@link TagLibrary} (a collection of {@link TagsetDefinition}s a.k.a. tagsets).
 */
public interface Project {
	/**
	 * The project emits these events to listeners that have been registered with
	 * {@link Project#addEventListener(ProjectEvent, PropertyChangeListener)}.
	 * <p>
	 * Note that {@link PropertyChangeListener} and {@link PropertyChangeEvent} are
	 * standard Java constructs whose names have no direct relation to CATMA tag properties.
	 */
	enum ProjectEvent {
		/**
		 * Signals an exception:
		 * <ul>
		 * <li>{@link PropertyChangeEvent#getNewValue()} = the {@link Exception}</li>
		 * </ul>
		 */
		exceptionOccurred,

		/**
		 * Signals a change in one or more properties of a tag instance:
		 * <ul>
		 * <li>{@link PropertyChangeEvent#getNewValue()} = a {@link Collection} of {@link Property}</li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = corresponding {@link TagInstance}</li>
		 * </ul>
		 */
		propertyValueChanged,

		/**
		 * Signals tag references having been added or removed.
		 * <br/><br/>
		 * <p>
		 * When added:
		 * <ul>
		 * <li>{@link PropertyChangeEvent#getNewValue()} = a {@link Pair} with the affected {@link AnnotationCollection}
		 * and a {@link List} of the new {@link TagReference}s</li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = <code>null</code></li>
		 * </ul>
		 * <p>
		 * When removed:
		 * <ul>
		 * <li>{@link PropertyChangeEvent#getNewValue()} = <code>null</code></li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = a {@link Pair} with the UUID string of the affected
		 * {@link AnnotationCollection} and a {@link Collection} of the old tag instance UUID strings</li>
		 * </ul>
		 */
		tagReferencesChanged
	}

	void open(OpenProjectListener openProjectListener);

	void close();

	/**
	 * Subscribes a listener to an event.
	 *
	 * @param projectEvent the {@link ProjectEvent} to listen for
	 * @param propertyChangeListener the {@link PropertyChangeListener}
	 */
	void addEventListener(ProjectEvent projectEvent, PropertyChangeListener propertyChangeListener);

	/**
	 * Unsubscribes a listener from an event.
	 *
	 * @param projectEvent the {@link ProjectEvent} that the listener is subscribed to
	 * @param propertyChangeListener the {@link PropertyChangeListener}
	 */
	void removeEventListener(ProjectEvent projectEvent, PropertyChangeListener propertyChangeListener);

	String getName();

	String getId();

	/**
	 * Adds a document to the project.
	 *
	 * @param sourceDocument the {@link SourceDocument} to add
	 * @throws IOException if an error occurs when adding the document
	 */
	void addSourceDocument(SourceDocument sourceDocument) throws IOException;

	/**
	 * Updates the metadata for a document.
	 *
	 * @param sourceDocumentRef a {@link SourceDocumentReference} specifying the document whose metadata should be updated
	 * @param contentInfoSet a {@link ContentInfoSet} containing the new metadata
	 * @param responsibleUser the username of the user responsible for the document
	 * @throws Exception if an error occurs when updating the document metadata
	 */
	void updateSourceDocumentMetadata(SourceDocumentReference sourceDocumentRef, ContentInfoSet contentInfoSet, String responsibleUser) throws Exception;

	/**
	 * Gets the document references for the documents in the project.
	 *
	 * @return a {@link Collection} of {@link SourceDocumentReference}
	 * @throws Exception if an error occurs when getting the document references
	 */
	Collection<SourceDocumentReference> getSourceDocumentReferences() throws Exception;

	/**
	 * Gets the tagsets in the project.
	 *
	 * @return a {@link Collection} of {@link TagsetDefinition}
	 * @throws Exception if an error occurs when getting the tagsets
	 */
	Collection<TagsetDefinition> getTagsets() throws Exception;

	/**
	 * Gets a single document by its ID.
	 *
	 * @param sourceDocumentId the ID of the document to get
	 * @return a {@link SourceDocument}
	 * @throws Exception if an error occurs when getting the document
	 */
	SourceDocument getSourceDocument(String sourceDocumentId) throws Exception;

	/**
	 * Deletes a document from the project.
	 *
	 * @param sourceDocument a {@link SourceDocumentReference} indicating the document to delete
	 * @throws Exception if an error occurs when deleting the document
	 */
	void deleteSourceDocument(SourceDocumentReference sourceDocument) throws Exception;

	/**
	 * Indicates whether the project contains a certain document.
	 *
	 * @param sourceDocumentId the ID of the document to check for
	 * @return true if the project contains the document, otherwise false
	 * @throws Exception if an error occurs when checking for the document
	 */
	boolean hasSourceDocument(String sourceDocumentId) throws Exception;

	/**
	 * Creates a new annotation collection within the project.
	 *
	 * @param name the name of the annotation collection to create
	 * @param sourceDocumentRef a {@link SourceDocumentReference} indicating the document to which the new collection relates
	 */
	void createAnnotationCollection(String name, SourceDocumentReference sourceDocumentRef);

	/**
	 * Gets a single annotation collection by its reference.
	 *
	 * @param annotationCollectionRef an {@link AnnotationCollectionReference} indicating the collection to get
	 * @return the {@link AnnotationCollection}
	 * @throws IOException if an error occurs when getting the collection
	 */
	AnnotationCollection getAnnotationCollection(AnnotationCollectionReference annotationCollectionRef) throws IOException;

	/**
	 * Adds tag references to an annotation collection.
	 *
	 * @param annotationCollection the {@link AnnotationCollection} to add the tag references to
	 * @param tagReferences the {@link TagReference}s to add
	 */
	void addTagReferencesToCollection(AnnotationCollection annotationCollection, List<TagReference> tagReferences);

	/**
	 * Removes tag references from an annotation collection.
	 *
	 * @param annotationCollection the {@link AnnotationCollection} to remove the tag references from
	 * @param tagReferences the {@link TagReference}s to remove
	 */
	void removeTagReferencesFromCollection(AnnotationCollection annotationCollection, List<TagReference> tagReferences);

	/**
	 * Updates the properties for a tag instance.
	 *
	 * @param annotationCollection the {@link AnnotationCollection} corresponding to the tag instance
	 * @param tagInstance the {@link TagInstance} to update
	 * @param properties the {@link Collection} of {@link Property} to update
	 */
	void updateTagInstanceProperties(AnnotationCollection annotationCollection, TagInstance tagInstance, Collection<Property> properties);

	/**
	 * Updates the metadata for an annotation collection.
	 *
	 * @param annotationCollectionRef an {@link AnnotationCollectionReference} specifying the collection whose metadata should be updated
	 * @param contentInfoSet a {@link ContentInfoSet} containing the new metadata
	 * @throws Exception if an error occurs when updating the collection metadata
	 */
	void updateAnnotationCollectionMetadata(AnnotationCollectionReference annotationCollectionRef, ContentInfoSet contentInfoSet) throws Exception;

	/**
	 * Deletes an annotation collection from the project.
	 *
	 * @param annotationCollectionRef an {@link AnnotationCollectionReference} indicating the collection to delete
	 * @throws Exception if an error occurs when deleting the collection
	 */
	void deleteAnnotationCollection(AnnotationCollectionReference annotationCollectionRef) throws Exception;

	/**
	 * Imports a tag library into the project.
	 *
	 * @param inputStream an {@link InputStream} for the tag library to import
	 * @return a {@link List} of {@link TagsetDefinitionImportStatus}
	 * @throws IOException if an error occurs when importing the tag library
	 */
	List<TagsetDefinitionImportStatus> importTagLibrary(InputStream inputStream) throws IOException;

	User getCurrentUser();

	TagManager getTagManager();

	Set<Member> getProjectMembers() throws IOException;

	boolean hasUncommittedChanges() throws Exception;

	void commitAndPushChanges(String commitMessage) throws Exception;

	void synchronizeWithRemote(OpenProjectListener openProjectListener) throws Exception;

	boolean hasPermission(RBACRole role, RBACPermission permission);

	void removeSubject(RBACSubject subject) throws IOException;

	RBACSubject assignRoleToSubject(RBACSubject subject, RBACRole role) throws IOException;

	List<User> findUser(String usernameOrEmail, int offset, int limit) throws IOException;

	String getDescription();

	RBACRole getCurrentUserProjectRole() throws IOException;

	void importTagsets(List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatuses) throws IOException;

	Pair<AnnotationCollection, List<TagsetDefinitionImportStatus>> importAnnotationCollection(
			InputStream inputStream,
			SourceDocumentReference sourceDocumentRef
	) throws IOException;

	void importAnnotationCollection(
			List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatuses,
			AnnotationCollection annotationCollection
	) throws IOException;

	void addSourceDocument(SourceDocument sourceDocument, boolean deleteTempFile) throws IOException;

	void addComment(Comment comment) throws IOException;

	void updateComment(Comment comment) throws IOException;

	void removeComment(Comment comment) throws IOException;

	List<Comment> getComments(String sourceDocumentId) throws IOException;

	void addCommentReply(Comment comment, Reply reply) throws IOException;

	List<Reply> getCommentReplies(Comment comment) throws IOException;

	void deleteCommentReply(Comment comment, Reply reply) throws IOException;

	void updateCommentReply(Comment comment, Reply reply) throws IOException;

	String getVersion();

	void addAndCommitCollections(Collection<AnnotationCollectionReference> annotationCollectionRefs, String commitMessage) throws IOException;

	SourceDocumentReference getSourceDocumentReference(String sourceDocumentId) throws Exception;

	void setLatestContributionView(boolean enabled, OpenProjectListener openProjectListener) throws Exception;

	boolean isReadOnly();
}
