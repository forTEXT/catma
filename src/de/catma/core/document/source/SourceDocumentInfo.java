package de.catma.core.document.source;


public class SourceDocumentInfo {

	private IndexInfoSet indexInfoSet;
	private ContentInfoSet contentInfoSet;
	private TechInfoSet techInfoSet;
	
	public SourceDocumentInfo() {
	}
	
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
	
	public void setTechInfoSet(TechInfoSet techInfoSet) {
		this.techInfoSet = techInfoSet;
	}
	
	public void setContentInfoSet(ContentInfoSet contentInfoSet) {
		this.contentInfoSet = contentInfoSet;
	}
	
	public void setIndexInfoSet(IndexInfoSet indexInfoSet) {
		this.indexInfoSet = indexInfoSet;
	}
}
