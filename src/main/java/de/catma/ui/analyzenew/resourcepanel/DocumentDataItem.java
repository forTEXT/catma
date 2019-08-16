package de.catma.ui.analyzenew.resourcepanel;

import com.vaadin.icons.VaadinIcons;

import de.catma.document.source.SourceDocument;

public class DocumentDataItem implements DocumentTreeItem {
	
	private SourceDocument document;
	private boolean selected;
	
	
	public DocumentDataItem(SourceDocument document, boolean selected) {
		super();
		this.document = document;
		this.selected = selected;
	}

	@Override
	public String getSelectionIcon() {
		return selected?VaadinIcons.DOT_CIRCLE.getHtml():VaadinIcons.CIRCLE_THIN.getHtml();
	}

	@Override
	public String getName() {
		return document.toString();
	}

	@Override
	public String getIcon() {
		return selected?VaadinIcons.OPEN_BOOK.getHtml():VaadinIcons.BOOK.getHtml();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((document == null) ? 0 : document.hashCode());
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
		if (document == null) {
			if (other.document != null)
				return false;
		} else if (!document.equals(other.document))
			return false;
		return true;
	}


	public void setSelected(boolean value) {
		this.selected = value;
	}

	public SourceDocument getDocument() {
		return document;
	}

	public boolean isSelected() {
		return selected;
	}
	

}
