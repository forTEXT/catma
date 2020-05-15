package de.catma.ui.module.project.corpusimport;

public class CorpusImportMetadata {
	private String ID;
	private String name;
	private CorpusImportDocumentMetadata[] contents;
	
	@Override
	public String toString() {
		return name;
	}

	public String getID() {
		return ID;
	}

	public String getName() {
		return name;
	}

	public CorpusImportDocumentMetadata[] getContents() {
		return contents;
	}
	
	
	
}