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


	String getId();

	String getName();

	String getDescription();

	String getVersion();

	User getCurrentUser();

	TagManager getTagManager();


	/**
	 * Whether this project is read-only or not. This is determined by the current view mode
	 * (see {@link Project#setLatestContributionView(boolean, OpenProjectListener)})
	 *
	 * @return true if this project is read-only, otherwise false
	 */
	boolean isReadOnly();

	/**
	 * Changes the view mode for this project, reporting progress and completion or failure to the provided listener.
	 * <p>
	 * The view mode is either 'synchronized' (the default) or 'latest contributions'.
	 *
	 * @param enabled whether to change to the 'latest contributions' view mode (will change to the 'synchronized' view mode if false)
	 * @param openProjectListener an {@link OpenProjectListener}
	 * @throws Exception if an error occurs when changing view modes
	 */
	void setLatestContributionView(boolean enabled, OpenProjectListener openProjectListener) throws Exception;


	/**
	 * Opens this project, reporting progress and completion or failure to the provided listener.
	 *
	 * @param openProjectListener an {@link OpenProjectListener}
	 */
	void open(OpenProjectListener openProjectListener);

	void close();


	// tagset & tag operations
	/**
	 * Gets the tagsets in this project.
	 *
	 * @return a {@link Collection} of {@link TagsetDefinition}
	 */
	Collection<TagsetDefinition> getTagsets();

	/**
	 * Prepares a tag library for importation into this project.
	 *
	 * @param inputStream an {@link InputStream} for the TEI-XML tag library to import
	 * @return a {@link List} of {@link TagsetDefinitionImportStatus}es providing information about the tagsets
	 * and tags that may or may not already exist in this project
	 * @throws IOException if an error occurs when preparing the tag library
	 */
	List<TagsetDefinitionImportStatus> prepareTagLibraryForImport(InputStream inputStream) throws IOException;

	/**
	 * Imports tagsets into this project.
	 *
	 * @param tagsetDefinitionImportStatuses a {@link List} of {@link TagsetDefinitionImportStatus}es providing the tagsets to import
	 * @throws IOException if an error occurs when importing the tagsets
	 */
	void importTagsets(List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatuses) throws IOException;

	// collection operations
	/**
	 * Gets a single annotation collection from this project.
	 *
	 * @param annotationCollectionRef an {@link AnnotationCollectionReference} indicating the collection to get
	 * @return the {@link AnnotationCollection}
	 * @throws IOException if an error occurs when getting the collection
	 */
	AnnotationCollection getAnnotationCollection(AnnotationCollectionReference annotationCollectionRef) throws IOException;

	/**
	 * Creates a new annotation collection within this project.
	 *
	 * @param name the name of the annotation collection to create
	 * @param sourceDocumentRef a {@link SourceDocumentReference} indicating the document to which the new collection relates
	 */
	void createAnnotationCollection(String name, SourceDocumentReference sourceDocumentRef);

	/**
	 * Updates the metadata for an annotation collection.
	 *
	 * @param annotationCollectionRef an {@link AnnotationCollectionReference} which specifies the collection and contains the updated metadata
	 * @throws IOException if an error occurs when updating the collection metadata
	 */
	void updateAnnotationCollectionMetadata(AnnotationCollectionReference annotationCollectionRef) throws IOException;

	/**
	 * Deletes an annotation collection from this project.
	 *
	 * @param annotationCollectionRef an {@link AnnotationCollectionReference} indicating the collection to delete
	 * @throws Exception if an error occurs when deleting the collection
	 */
	void deleteAnnotationCollection(AnnotationCollectionReference annotationCollectionRef) throws Exception;

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
	 * Prepares a new annotation collection for importation into this project.
	 *
	 * @param inputStream an {@link InputStream} for the TEI-XML collection to import
	 * @param sourceDocumentRef a {@link SourceDocumentReference} indicating the document to which the collection should belong
	 * @return a {@link Pair} with the {@link AnnotationCollection} and a {@link List} of {@link TagsetDefinitionImportStatus}es
	 * providing information about the tagsets and tags that may or may not already exist in this project
	 * @throws IOException if an error occurs when preparing the collection
	 */
	Pair<AnnotationCollection, List<TagsetDefinitionImportStatus>> prepareAnnotationCollectionForImport(
			InputStream inputStream,
			SourceDocumentReference sourceDocumentRef
	) throws IOException;

	/**
	 * Imports an annotation collection into this project.
	 *
	 * @param tagsetDefinitionImportStatuses a {@link List} of {@link TagsetDefinitionImportStatus}es providing information
	 *                                       about the tagsets and tags that may or may not already exist in this project
	 * @param annotationCollection the {@link AnnotationCollection} to import
	 * @throws IOException if an error occurs when importing the collection
	 */
	void importAnnotationCollection(
			List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatuses,
			AnnotationCollection annotationCollection
	) throws IOException;

	// document operations
	/**
	 * Whether this project contains a certain document.
	 *
	 * @param sourceDocumentId the ID of the document to check for
	 * @return true if this project contains the document, otherwise false
	 */
	boolean hasSourceDocument(String sourceDocumentId);

	/**
	 * Gets the document references for the documents in this project.
	 *
	 * @return a {@link Collection} of {@link SourceDocumentReference}
	 * @throws Exception if an error occurs when getting the document references
	 */
	Collection<SourceDocumentReference> getSourceDocumentReferences() throws Exception;

	/**
	 * Gets the document reference for a particular document in this project.
	 *
	 * @param sourceDocumentId the ID of the document for which to get the reference
	 * @return a {@link SourceDocumentReference}
	 */
	SourceDocumentReference getSourceDocumentReference(String sourceDocumentId);

	/**
	 * Gets a single document from this project.
	 *
	 * @param sourceDocumentId the ID of the document to get
	 * @return a {@link SourceDocument}
	 * @throws Exception if an error occurs when getting the document
	 */
	SourceDocument getSourceDocument(String sourceDocumentId) throws Exception;

	/**
	 * Adds a document to this project.
	 *
	 * @param sourceDocument the {@link SourceDocument} to add
	 * @throws IOException if an error occurs when adding the document
	 */
	void addSourceDocument(SourceDocument sourceDocument) throws Exception;

	/**
	 * Adds a document to this project.
	 *
	 * @param sourceDocument the {@link SourceDocument} to add
	 * @param deleteTempFile whether to delete the associated temp file from the disk
	 * @throws IOException if an error occurs when adding the document
	 */
	void addSourceDocument(SourceDocument sourceDocument, boolean deleteTempFile) throws Exception;

	/**
	 * Updates the metadata for a document.
	 *
	 * @param sourceDocumentRef a {@link SourceDocumentReference} which specifies the document and contains the updated metadata
	 * @throws IOException if an error occurs when updating the document metadata
	 */
	void updateSourceDocumentMetadata(SourceDocumentReference sourceDocumentRef) throws IOException;

	/**
	 * Deletes a document from this project.
	 *
	 * @param sourceDocument a {@link SourceDocumentReference} indicating the document to delete
	 * @throws Exception if an error occurs when deleting the document
	 */
	void deleteSourceDocument(SourceDocumentReference sourceDocument) throws Exception;

	// comment operations
	/**
	 * Gets the document comments for a particular document.
	 *
	 * @param sourceDocumentId the ID of the document for which comments should be fetched
	 * @return a {@link List} of {@link Comment}s
	 * @throws IOException if an error occurs when getting comments
	 */
	List<Comment> getComments(String sourceDocumentId) throws IOException;

	/**
	 * Adds a document comment to this project.
	 * <p>
	 * Note: the document is specified in the comment itself.
	 *
	 * @param comment a {@link Comment}
	 * @throws IOException if an error occurs when adding the comment
	 */
	void addComment(Comment comment) throws IOException;

	/**
	 * Updates a document comment in this project.
	 * <p>
	 * Note: the document is specified in the comment itself.
	 *
	 * @param comment the {@link Comment} to update
	 * @throws IOException if an error occurs when updating the comment
	 */
	void updateComment(Comment comment) throws IOException;

	/**
	 * Removes a document comment from this project.
	 * <p>
	 * Note: the document is specified in the comment itself.
	 *
	 * @param comment the {@link Comment} to remove
	 * @throws IOException if an error occurs when removing the comment
	 */
	void removeComment(Comment comment) throws IOException;

	/**
	 * Gets the replies for a comment.
	 *
	 * @param comment the {@link Comment} for which to get the replies
	 * @return a {@link List} of {@link Reply}
	 * @throws IOException if an error occurs when getting the replies
	 */
	List<Reply> getCommentReplies(Comment comment) throws IOException;

	/**
	 * Adds a reply to a document comment.
	 * <p>
	 * Note: the document is specified in the comment itself.
	 *
	 * @param comment the {@link Comment} to which the reply should be added
	 * @param reply a {@link Reply}
	 * @throws IOException if an error occurs when adding the reply
	 */
	void addCommentReply(Comment comment, Reply reply) throws IOException;

	/**
	 * Updates a reply to a document comment.
	 * <p>
	 * Note: the document is specified in the comment itself.
	 *
	 * @param comment the {@link Comment} whose reply should be updated
	 * @param reply the {@link Reply} to update
	 * @throws IOException if an error occurs when updating the reply
	 */
	void updateCommentReply(Comment comment, Reply reply) throws IOException;

	/**
	 * Deletes a reply from a document comment.
	 * <p>
	 * Note: the document is specified in the comment itself.
	 *
	 * @param comment the {@link Comment} from which the reply should be deleted
	 * @param reply the {@link Reply} to delete
	 * @throws IOException if an error occurs when deleting the reply
	 */
	void deleteCommentReply(Comment comment, Reply reply) throws IOException;

	// member, role and permissions related things
	// TODO: strictly speaking findUser & hasPermission don't belong in this interface as they don't relate to a particular project
	/**
	 * Searches for users amongst all available users.
	 *
	 * @param usernameOrEmail the partial or complete username or email address to search for
	 * @return a {@link List} of {@link User}s
	 * @throws IOException if an error occurs when searching
	 */
	List<User> findUser(String usernameOrEmail) throws IOException;

	/**
	 * Whether the given role has the given permission.
	 *
	 * @param role an {@link RBACRole}
	 * @param permission an {@link RBACPermission}
	 * @return true if the role has the permission, otherwise false
	 */
	boolean hasPermission(RBACRole role, RBACPermission permission);

	/**
	 * Gets the role that the current user has on this project.
	 *
	 * @return an {@link RBACRole}
	 * @throws IOException if an error occurs when getting the role
	 */
	RBACRole getCurrentUserProjectRole() throws IOException;

	/**
	 * Gets the members of this project.
	 *
	 * @return a {@link Set} of {@link Member}
	 * @throws IOException if an error occurs when getting the project members
	 */
	Set<Member> getProjectMembers() throws IOException;

	/**
	 * Adds a new member to this project.
	 *
	 * @param subject the {@link RBACSubject} to add
	 * @param role the {@link RBACRole} to assign
	 * @return the {@link RBACSubject} that was added
	 * @throws IOException if an error occurs when adding the member
	 */
	RBACSubject assignRoleToSubject(RBACSubject subject, RBACRole role) throws IOException;

	/**
	 * Removes a member from this project.
	 *
	 * @param subject the {@link RBACSubject} to remove
	 * @throws IOException if an error occurs when removing the member
	 */
	void removeSubject(RBACSubject subject) throws IOException;

	// synchronization related things
	// TODO: strictly speaking none of these belong in this interface as they are implementation-specific
	/**
	 * Whether this project has any uncommitted changes.
	 *
	 * @return true if this project has uncommitted changes, otherwise false
	 * @throws Exception if an error occurs when determining whether there are uncommitted changes
	 */
	boolean hasUncommittedChanges() throws Exception;

	/**
	 * Commits and pushes the uncommitted changes in this project.
	 *
	 * @param commitMessage the commit message
	 * @throws IOException if an error occurs when committing or pushing changes
	 */
	void commitAndPushChanges(String commitMessage) throws IOException;

	/**
	 * Synchronizes this project with the remote server, reporting progress and completion or failure to the provided listener.
	 *
	 * @param openProjectListener an {@link OpenProjectListener}
	 * @throws Exception if an error occurs when synchronizing
	 */
	void synchronizeWithRemote(OpenProjectListener openProjectListener) throws Exception;

	/**
	 * Adds (i.e. git add) and commits the specified annotation collections.
	 *
	 * @param annotationCollectionRefs a {@link Collection} of {@link AnnotationCollectionReference}s indicating the collections to add and commit
	 * @param commitMessage the commit message
	 * @throws IOException if an error occurs when adding and committing the collections
	 */
	void addAndCommitCollections(Collection<AnnotationCollectionReference> annotationCollectionRefs, String commitMessage) throws IOException;
}
