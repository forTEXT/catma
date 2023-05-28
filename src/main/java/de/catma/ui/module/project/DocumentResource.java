package de.catma.ui.module.project;

import com.vaadin.icons.VaadinIcons;
import de.catma.document.annotation.AnnotationCollectionReference;
import de.catma.document.source.SourceDocumentReference;
import de.catma.project.Project;
import de.catma.user.User;

public class DocumentResource implements Resource {
	private final SourceDocumentReference sourceDocumentRef;
	private final String projectId;
	private final User responsibleUser;

	public DocumentResource(SourceDocumentReference sourceDocumentRef, String projectId, User responsibleUser) {
		this.sourceDocumentRef = sourceDocumentRef;
		this.projectId = projectId;
		this.responsibleUser = responsibleUser;
	}

	@Override
	public String getResourceId() {
		return sourceDocumentRef.getUuid();
	}

	@Override
	public String getProjectId() {
		return projectId;
	}

	@Override
	public String getName() {
		return sourceDocumentRef.toString();
	}

	@Override
	public String getDetail() {
		return sourceDocumentRef.getSourceDocumentInfo().getContentInfoSet().getAuthor();
	}

	@Override
	public boolean hasDetail() {
		String author = sourceDocumentRef.getSourceDocumentInfo().getContentInfoSet().getAuthor();
		return author != null && !author.trim().isEmpty();
	}

	@Override
	public String getIcon() {
		return VaadinIcons.BOOK.getHtml();
	}

	@Override
	public void deleteFrom(Project project) throws Exception {
		project.deleteSourceDocument(sourceDocumentRef);
	}

	@Override
	public String getResponsibleUser() {
		return responsibleUser != null ? responsibleUser.getName() : "Not assigned";
	}

	@Override
	public boolean isResponsible(String userIdentifier) {
		for (AnnotationCollectionReference annotationCollectionRef : sourceDocumentRef.getUserMarkupCollectionRefs()) {
			if (!annotationCollectionRef.isResponsible(userIdentifier)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean isContribution() {
		return sourceDocumentRef.isContribution();
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sourceDocumentRef == null) ? 0 : sourceDocumentRef.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DocumentResource)) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		DocumentResource other = (DocumentResource) obj;
		if (sourceDocumentRef == null) {
			return other.sourceDocumentRef == null;
		}
		else {
			return sourceDocumentRef.equals(other.sourceDocumentRef);
		}
	}

	public SourceDocumentReference getSourceDocumentRef() {
		return sourceDocumentRef;
	}
}
