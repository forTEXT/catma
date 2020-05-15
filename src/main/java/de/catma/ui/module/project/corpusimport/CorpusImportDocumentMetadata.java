package de.catma.ui.module.project.corpusimport;

public class CorpusImportDocumentMetadata {
	
	private String sourceDocID;
	private String sourceDocName;
	private String sourceDocAuthor;
	private String sourceDocDescription;
	private String sourceDocPublisher;
	private String sourceDocLocale;
	private String[] sourceDocSepChars;
	private String[] sourceDocUnsepSeqs;
	private CorpusImportCollectionMetadata[] umcList;
	
	public String getSourceDocID() {
		return sourceDocID;
	}
	public String getSourceDocName() {
		return sourceDocName;
	}
	public String getSourceDocAuthor() {
		return sourceDocAuthor;
	}
	public String getSourceDocDescription() {
		return sourceDocDescription;
	}
	public String getSourceDocPublisher() {
		return sourceDocPublisher;
	}
	public String getSourceDocLocale() {
		return sourceDocLocale;
	}
	public String[] getSourceDocSepChars() {
		return sourceDocSepChars;
	}
	public String[] getSourceDocUnsepSeqs() {
		return sourceDocUnsepSeqs;
	}
	public CorpusImportCollectionMetadata[] getUmcList() {
		return umcList;
	}
	
	
}