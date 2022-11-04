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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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
import de.catma.tag.Property;
import de.catma.tag.TagInstance;
import de.catma.tag.TagLibrary;
import de.catma.tag.TagManager;
import de.catma.tag.TagsetDefinition;
import de.catma.user.Member;
import de.catma.user.User;
import de.catma.util.Pair;

/**
 * A repository to store {@link SourceDocument}s, {@link AnnotationCollection}s and
 * {@link TagLibrary TagLibraries}.
 * 
 * @author marco.petris@web.de
 *
 */
public interface Project {
	
	/**
	 * The Repository emits these change events to listeners that have
	 * been registered with {@link Repository#addPropertyChangeListener(RepositoryChangeEvent, PropertyChangeListener)}.
	 *
	 */
	/**
	 * @author marco.petris@web.de
	 *
	 */
	public static enum RepositoryChangeEvent {
		/**
		 * Signals an exception:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = the {@link Exception}</li>
		 */
		exceptionOccurred, 
		/**
		 * <p>{@link Property} changed:
		 * <li>{@link PropertyChangeEvent#getNewValue()} =  {@link List} of {@link Property Properties}</li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = corresponding {@link TagInstance}</li>
		 * </p>
		 */
		propertyValueChanged,
		/**
		 * <p>{@link TagReference}s added:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = a {@link Pair} of 
		 * a {@link UserMarkupCollection} and a {@link List} of {@link TagReference}s</li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = <code>null</code></li>
		 * </p><br />
		 * <p>{@link TagReference}s removed:
		 * <li>{@link PropertyChangeEvent#getNewValue()} = <code>null</code></li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = a {@link Pair} of an 
		 * UUID String of a {@link UserMarkupCollection} and a {@link Collection} of Annotation ID Strings</li>
		 * </p><br />
		 */
		tagReferencesChanged,
		/**
		 * <p> a notification to the repo holder.
		 * <li>{@link PropertyChangeEvent#getNewValue()} = the message of type String</li>
		 * <li>{@link PropertyChangeEvent#getOldValue()} = always <code>null</code></li>
		 * </p>
		 */
		notification,
		;
	}
	
	/**
	 */
	public void open(OpenProjectListener openProjectListener);
	
	public void close();
	
	/**
	 * @param propertyChangeEvent event to listen for
	 * @param propertyChangeListener
	 */
	public void addPropertyChangeListener(
			RepositoryChangeEvent propertyChangeEvent, 
			PropertyChangeListener propertyChangeListener);
	
	public void removePropertyChangeListener(
			RepositoryChangeEvent propertyChangeEvent, 
			PropertyChangeListener propertyChangeListener);
	
	/**
	 * @return name of the repository
	 */
	public String getName();
	
	
	public String getProjectId();

	/**
	 * Inserts the given SourceDocument into the repository.
	 * @param sourceDocument
	 */
	public void insert(SourceDocument sourceDocument) throws IOException;

	/**
	 * Updates the metadata for a document.
	 *
	 * @param sourceDocumentRef a {@link SourceDocumentReference} specifying the document whose metadata should be updated
	 * @param contentInfoSet a {@link ContentInfoSet} containing the new metadata
	 * @param responsibleUser the username of the user responsible for the document
	 * @throws Exception if an error occurs when updating the document metadata
	 */
	void updateDocumentMetadata(SourceDocumentReference sourceDocumentRef, ContentInfoSet contentInfoSet, String responsibleUser) throws Exception;

	/**
	 * @return the available Source Documents
	 * @throws Exception 
	 */
	public Collection<SourceDocumentReference> getSourceDocumentReferences() throws Exception;

	/**
	 *
	 * @return the available Tagsets
	 * @throws Exception
	 */
	Collection<TagsetDefinition> getTagsets() throws Exception;


	/**
	 * @param id ID of the SourceDocument
	 * @return the SourceDocument with the given ID
	 * @throws Exception 
	 */
	public SourceDocument getSourceDocument(String id) throws Exception;
	public void delete(SourceDocumentReference sourceDocument) throws Exception;

	public boolean hasDocument(String documentId) throws Exception;

	/**
	 * Creates a User Markup Collection with that name for the given Source Document.
	 * @param name
	 * @param sourceDocument
	 * @throws IOException
	 */
	public void createUserMarkupCollection(String name, SourceDocumentReference sourceDocument);

	/**
	 * @param userMarkupCollectionReference
	 * @return the User Markup Collection for the given reference.
	 * @throws IOException
	 */
	public AnnotationCollection getUserMarkupCollection(
			AnnotationCollectionReference userMarkupCollectionReference) throws IOException;

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
	 * @param properties the {@link Property}s to update
	 */
	void updateTagInstanceProperties(AnnotationCollection annotationCollection, TagInstance tagInstance, Collection<Property> properties);

	/**
	 * Updates the metadata for an annotation collection.
	 *
	 * @param annotationCollectionRef an {@link AnnotationCollectionReference} specifying the collection whose metadata should be updated
	 * @param contentInfoSet a {@link ContentInfoSet} containing the new metadata
	 */
	void updateAnnotationCollectionMetadata(AnnotationCollectionReference annotationCollectionRef, ContentInfoSet contentInfoSet) throws Exception;

	public void delete(
			AnnotationCollectionReference userMarkupCollectionReference) throws Exception;

	/**
	 * @param inputStream the tag library
	 * @return 
	 * @throws IOException
	 */
	public List<TagsetDefinitionImportStatus> loadTagLibrary(InputStream inputStream) throws IOException;

	/**
	 * @return current user of this repository instance
	 */
	public User getUser();
	
	/**
	 * @return the Tag Manager for this repository
	 */
	public TagManager getTagManager();
		
	public Set<Member> getProjectMembers() throws IOException;

	public boolean hasUncommittedChanges() throws Exception;

	public boolean hasUntrackedChanges() throws IOException;

	public boolean hasChangesToCommitOrPush() throws Exception;

	public void commitAndPushChanges(String commitMsg) throws Exception;

	public void synchronizeWithRemote(OpenProjectListener openProjectListener) throws Exception;

	void printStatus();

	boolean hasPermission(RBACRole role, RBACPermission permission);

	boolean isAuthorizedOnProject(RBACPermission permission);

	void unassignFromProject(RBACSubject subject) throws IOException;

	RBACSubject assignOnProject(RBACSubject subject, RBACRole role) throws IOException;

	List<User> findUser(String usernameOrEmail, int offset, int limit) throws IOException;

	String getDescription();

	RBACRole getRoleOnProject() throws IOException;

	public void importTagsets(List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatusList) throws IOException;

	public Pair<AnnotationCollection, List<TagsetDefinitionImportStatus>> loadAnnotationCollection(
			InputStream inputStream, SourceDocumentReference documentRef) throws IOException;

	void importCollection(List<TagsetDefinitionImportStatus> tagsetDefinitionImportStatuses, AnnotationCollection annotationCollection) throws IOException;

	void insert(SourceDocument sourceDocument, boolean deleteTempFile) throws IOException;

	public List<CommitInfo> getUnsynchronizedCommits() throws Exception;

	public void addComment(Comment comment) throws IOException;

	public void updateComment(Comment comment) throws IOException;

	public void removeComment(Comment comment) throws IOException;

	List<Comment> getComments(String documentId) throws IOException;

	public void addReply(Comment comment, Reply reply) throws IOException;

	public List<Reply> getCommentReplies(Comment comment) throws IOException;
	
	void removeReply(Comment comment, Reply reply) throws IOException;

	void updateReply(Comment comment, Reply reply) throws IOException;

	String getVersion();

	public void addAndCommitCollections(
			Collection<AnnotationCollectionReference> collectionReferences, String msg) throws IOException;

	public SourceDocumentReference getSourceDocumentReference(String sourceDocumentID) throws Exception;

	public void setLatestContributionView(boolean enabled, OpenProjectListener openProjectListener) throws Exception;

	boolean isReadOnly();

}
