package de.catma.core.document.source;

public class SourceDocumentInfo {

	private IndexInfoSet indexInfoSet;
	private ContentInfoSet contentInfoSet;
	private TechInfoSet techInfoSet;
	public SourceDocumentInfo(IndexInfoSet indexInfoSet,
			ContentInfoSet contentInfoSet, TechInfoSet techInfoSet) {
		super();
		this.indexInfoSet = indexInfoSet;
		this.contentInfoSet = contentInfoSet;
		this.techInfoSet = techInfoSet;
	}
	public IndexInfoSet getIndexInfoSet() {
		return indexInfoSet;
	}
	public ContentInfoSet getContentInfoSet() {
		return contentInfoSet;
	}
	public TechInfoSet getTechInfoSet() {
		return techInfoSet;
	}
	
	
}
