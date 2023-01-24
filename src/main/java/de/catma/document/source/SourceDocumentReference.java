package de.catma.document.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.catma.document.annotation.AnnotationCollection;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.contenthandler.SourceContentHandler;

public class SourceDocumentReference {
	private final String uuid;
	private SourceContentHandler sourceContentHandler;
	private List<AnnotationCollectionReference> userMarkupCollectionRefs;
	
	
	/**
	 * @param uuid identifier for this document
	 * @param handler the appropriate content handler
	 * @see SourceDocumentHandler
	 */
	public SourceDocumentReference(String uuid, SourceContentHandler handler) {
		this.uuid = uuid;
		this.sourceContentHandler = handler;
		this.userMarkupCollectionRefs = new ArrayList<AnnotationCollectionReference>();
	}

	/**
	 * displays title or id
	 */
	@Override
	public String toString() {
		String title = 
				sourceContentHandler.getSourceDocumentInfo().getContentInfoSet().getTitle();
		return ((title == null) || (title.isEmpty()))? uuid : title;
	}

	/**
	 * Attaches a collection of user defined markup to this document.
	 * @param userMarkupCollRef user markup
	 */
	public void addUserMarkupCollectionReference(
			AnnotationCollectionReference userMarkupCollRef) {
		userMarkupCollectionRefs.add(userMarkupCollRef);
	}

	/**
	 * @return the identifier of this document, depending on the underlying repository
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * @return all user defined markup attached
	 */
	public List<AnnotationCollectionReference> getUserMarkupCollectionRefs() {
		return Collections.unmodifiableList(userMarkupCollectionRefs);
	}
	
	/**
	 * @param id the identifier of the {@link AnnotationCollection}
	 * @return the reference to the user markup collection or <code>null</code> if
	 * there is no such collection
	 */
	public AnnotationCollectionReference getUserMarkupCollectionReference(String id) {
		for (AnnotationCollectionReference ref : userMarkupCollectionRefs) {
			if (ref.getId().equals(id)) {
				return ref;
			}
		}
		return null;
	}
	
	/**
	 * @param uRef to be removed
	 * @return true if the uRef had been attached before
	 */
	public boolean removeUserMarkupCollectionReference(
			AnnotationCollectionReference uRef) {
		return this.userMarkupCollectionRefs.remove(uRef);
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
		if (!(obj instanceof SourceDocumentReference))
			return false;
		SourceDocumentReference other = (SourceDocumentReference) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

	public SourceDocumentInfo getSourceDocumentInfo() {
		return sourceContentHandler.getSourceDocumentInfo();
	}

	public boolean isResponsible(String userIdentifier) {
		String responsibleUser = sourceContentHandler.getSourceDocumentInfo().getTechInfoSet().getResponsibleUser();

		if (responsibleUser != null) {
			return responsibleUser.equals(userIdentifier);
		}

		return true; // shared responsibility
	}

	public String getResponsibleUser() {
		return sourceContentHandler.getSourceDocumentInfo().getTechInfoSet().getResponsibleUser();
	}

	public void setResponsibleUser(String responsibleUser) {
		sourceContentHandler.getSourceDocumentInfo().getTechInfoSet().setResponsibleUser(responsibleUser);
	}
}
