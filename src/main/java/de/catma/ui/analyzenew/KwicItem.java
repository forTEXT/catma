package de.catma.ui.analyzenew;


public class KwicItem {
	
	private String sourceDocOrMarkupCollectionDisplay;
	private String backwardContext;
	private String keyWord;
	private String forewardContext;
	private String tagDefinitionPath;
	private String propertyName;
	private String propertyValue;
	private String docCollection;
	private int rangeStartPoint;
	private int rangeEndPoint;
	
	
	public KwicItem() {
		
	}

	
	
	
	public String getSourceDocOrMarkupCollectionDisplay() {
		return sourceDocOrMarkupCollectionDisplay;
	}
	public void setSourceDocOrMarkupCollectionDisplay(String sourceDocOrMarkupCollectionDisplay) {
		this.sourceDocOrMarkupCollectionDisplay = sourceDocOrMarkupCollectionDisplay;
	}
	public String getBackwardContext() {
		return backwardContext;
	}
	public void setBackwardContext(String backwardContext) {
		this.backwardContext = backwardContext;
	}

	public String getKeyWord() {
		return keyWord;
	}
	public void setKeyWord(String keyWord) {
		this.keyWord = keyWord;
	}
	public String getForewardContext() {
		return forewardContext;
	}
	public void setForewardContext(String forewardContext) {
		this.forewardContext = forewardContext;
	}
	public String getTagDefinitionPath() {
		return tagDefinitionPath;
	}
	public void setTagDefinitionPath(String tagDefinitionPath) {
		this.tagDefinitionPath = tagDefinitionPath;
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
	public int getRangeStartPoint() {
		return rangeStartPoint;
	}
	public void setRangeStartPoint(int rangeStartPoint) {
		this.rangeStartPoint = rangeStartPoint;
	}
	public int getRangeEndPoint() {
		return rangeEndPoint;
	}
	public void setRangeEndPoint(int rangeEndPoint) {
		this.rangeEndPoint = rangeEndPoint;
	}




	public String getDocCollection() {
		return docCollection;
	}




	public void setDocCollection(String docCollection) {
		this.docCollection = docCollection;
	}
	
	
	
	
	

}
