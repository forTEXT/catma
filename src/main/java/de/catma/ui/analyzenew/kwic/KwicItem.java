package de.catma.ui.analyzenew.kwic;


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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((backwardContext == null) ? 0 : backwardContext.hashCode());
		result = prime * result + ((docCollection == null) ? 0 : docCollection.hashCode());
		result = prime * result + ((forewardContext == null) ? 0 : forewardContext.hashCode());
		result = prime * result + ((keyWord == null) ? 0 : keyWord.hashCode());
		result = prime * result + ((propertyName == null) ? 0 : propertyName.hashCode());
		result = prime * result + ((propertyValue == null) ? 0 : propertyValue.hashCode());
		result = prime * result + rangeEndPoint;
		result = prime * result + rangeStartPoint;
		result = prime * result
				+ ((sourceDocOrMarkupCollectionDisplay == null) ? 0 : sourceDocOrMarkupCollectionDisplay.hashCode());
		result = prime * result + ((tagDefinitionPath == null) ? 0 : tagDefinitionPath.hashCode());
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
		KwicItem other = (KwicItem) obj;
		if (backwardContext == null) {
			if (other.backwardContext != null)
				return false;
		} else if (!backwardContext.equals(other.backwardContext))
			return false;
		if (docCollection == null) {
			if (other.docCollection != null)
				return false;
		} else if (!docCollection.equals(other.docCollection))
			return false;
		if (forewardContext == null) {
			if (other.forewardContext != null)
				return false;
		} else if (!forewardContext.equals(other.forewardContext))
			return false;
		if (keyWord == null) {
			if (other.keyWord != null)
				return false;
		} else if (!keyWord.equals(other.keyWord))
			return false;
		if (propertyName == null) {
			if (other.propertyName != null)
				return false;
		} else if (!propertyName.equals(other.propertyName))
			return false;
		if (propertyValue == null) {
			if (other.propertyValue != null)
				return false;
		} else if (!propertyValue.equals(other.propertyValue))
			return false;
		if (rangeEndPoint != other.rangeEndPoint)
			return false;
		if (rangeStartPoint != other.rangeStartPoint)
			return false;
		if (sourceDocOrMarkupCollectionDisplay == null) {
			if (other.sourceDocOrMarkupCollectionDisplay != null)
				return false;
		} else if (!sourceDocOrMarkupCollectionDisplay.equals(other.sourceDocOrMarkupCollectionDisplay))
			return false;
		if (tagDefinitionPath == null) {
			if (other.tagDefinitionPath != null)
				return false;
		} else if (!tagDefinitionPath.equals(other.tagDefinitionPath))
			return false;
		return true;
	}		

}
