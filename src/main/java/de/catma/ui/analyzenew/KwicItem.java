package de.catma.ui.analyzenew;


public class KwicItem {
	
	static final String HORIZONTAL_ELLIPSIS = "\u2026";
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
	
	public String getShortenKeyWord() {
		return shorten(keyWord,12);
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
	
	public String getShortenTagDefinitionPath() {
		if(tagDefinitionPath!=null)
		return shorten(tagDefinitionPath,12);
		return null;
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
	
	private String shorten(String toShortenValue, int maxLength) {
		if (toShortenValue.length() <= maxLength) {
			return toShortenValue;
		}else {
			return toShortenValue.substring(0, maxLength / 2) + "[" + HORIZONTAL_ELLIPSIS + "]"
					+ toShortenValue.substring(toShortenValue.length() - ((maxLength / 2) - 2), toShortenValue.length());
			
		}
		
	}		

}
