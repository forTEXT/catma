package de.catma.ui.module.annotate.resourcepanel;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.source.SourceDocumentReference;

class DocumentDataItem implements DocumentTreeItem {
	
	private SourceDocumentReference documentReference;
	private boolean selected;
	
	
	public DocumentDataItem(SourceDocumentReference documentReference, boolean selected) {
		super();
		this.documentReference = documentReference;
		this.selected = selected;
	}

	@Override
	public String getSelectionIcon() {
		return selected?VaadinIcons.DOT_CIRCLE.getHtml():VaadinIcons.CIRCLE_THIN.getHtml();
	}

	@Override
	public String getName() {
		return documentReference.toString();
	}

	@Override
	public String getIcon() {
		return selected?VaadinIcons.OPEN_BOOK.getHtml():VaadinIcons.BOOK.getHtml();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((documentReference == null) ? 0 : documentReference.hashCode());
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
		DocumentDataItem other = (DocumentDataItem) obj;
		if (documentReference == null) {
			if (other.documentReference != null)
				return false;
		} else if (!documentReference.equals(other.documentReference))
			return false;
		return true;
	}

	@Override
	public void setSelected(boolean value) {
		this.selected = value;
	}
	
	@Override
	public boolean isSingleSelection() {
		return true;
	}

	public SourceDocumentReference getDocument() {
		return documentReference;
	}
	
	@Override
	public boolean isSelected() {
		return selected;
	}
	
	@Override
	public String toString() {
		return documentReference.toString();
	}
	
	@Override
	public void fireSelectedEvent(ResourceSelectionListener resourceSelectionListener) {
		resourceSelectionListener.documentSelected(documentReference);
	}

}
