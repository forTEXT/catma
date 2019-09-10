package de.catma.ui.client.ui.visualization.vega.shared;

public class SelectedQueryResultRow {
	
	private String queryId;
	private String sourceDocumentId;
	private int startOffset;
	private int endOffset;
	private String phrase;
	private String annotationCollectionId;
	private String tagId;
	private String tagPath;
	private String tagVersion;
	private String annotationId;
	private String propertyId;
	private String propertyName;
	private String propertyValue;
	
	public SelectedQueryResultRow() {
	}
	
	public String getSourceDocumentId() {
		return sourceDocumentId;
	}
	public void setSourceDocumentId(String sourceDocumentId) {
		this.sourceDocumentId = sourceDocumentId;
	}

	public int getStartOffset() {
		return startOffset;
	}
	public void setStartOffset(int startOffset) {
		this.startOffset = startOffset;
	}
	public int getEndOffset() {
		return endOffset;
	}
	public void setEndOffset(int endOffset) {
		this.endOffset = endOffset;
	}
	public String getPhrase() {
		return phrase;
	}
	public void setPhrase(String phrase) {
		this.phrase = phrase;
	}
	public String getAnnotationCollectionId() {
		return annotationCollectionId;
	}
	public void setAnnotationCollectionId(String annotationCollectionId) {
		this.annotationCollectionId = annotationCollectionId;
	}
	public String getTagId() {
		return tagId;
	}
	public void setTagId(String tagId) {
		this.tagId = tagId;
	}
	public String getTagPath() {
		return tagPath;
	}
	public void setTagPath(String tagPath) {
		this.tagPath = tagPath;
	}
	public String getTagVersion() {
		return tagVersion;
	}
	public void setTagVersion(String tagVersion) {
		this.tagVersion = tagVersion;
	}
	public String getAnnotationId() {
		return annotationId;
	}
	public void setAnnotationId(String annotationId) {
		this.annotationId = annotationId;
	}
	public String getPropertyId() {
		return propertyId;
	}
	public void setPropertyId(String propertyId) {
		this.propertyId = propertyId;
	}
	public String getPropertyName() {
		return propertyName;
	}
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}
	public String getPropertyValue() {
		return propertyValue;
	}
	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}
	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}
	public String getQueryId() {
		return queryId;
	}
}
