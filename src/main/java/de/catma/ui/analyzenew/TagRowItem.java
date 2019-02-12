package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.UUID;

import de.catma.queryengine.result.QueryResultRow;

public class TagRowItem {
	
	static final String HORIZONTAL_ELLIPSIS = "\u2026";
	static final int MAX_VALUE_LENGTH = 10;
	static final int maxLength = 50;
	private String tagID;
	private String sourceDocumentID;
	private String sourceDocName;
	private String collectionID;
	private String collectionName;
	private String tagInstanceID;
	private String tagDefinitionID;
	private String tagDefinitionPath;
	private ArrayList <TagRowItem>children;
	private UUID uuid;
	private String treePath;
	private Integer frequency;
	private String propertyName;
	private Object propertyValue;
	private QueryResultRow queryResultRow; 
	private String phrase;

	
	public TagRowItem() {
		//uuid = UUID.randomUUID();

		
		
	}
	public String getTagID() {
		return tagID;
	}
	public void setTagID(String tagID) {
		this.tagID = tagID;
	}
	public String getSourceDocumentID() {
		return sourceDocumentID;
	}
	public void setSourceDocumentID(String sourceDocumentID) {
		this.sourceDocumentID = sourceDocumentID;
	}
	public String getSourceDocName() {
		return sourceDocName;
	}
	public void setSourceDocName(String sourceDocName) {
		this.sourceDocName = sourceDocName;
	}
	public String getCollectionID() {
		return collectionID;
	}
	public void setCollectionID(String collectionID) {
		this.collectionID = collectionID;
	}
	public String getTagInstanceID() {
		return tagInstanceID;
	}
	public void setTagInstanceID(String tagInstanceID) {
		this.tagInstanceID = tagInstanceID;
	}
	public String getTagDefinitionID() {
		return tagDefinitionID;
	}
	public void setTagDefinitionID(String tagDefinitionID) {
		this.tagDefinitionID = tagDefinitionID;
	}	
	public String getTagDefinitionPath() {
		return tagDefinitionPath;
	}
	public void setTagDefinitionPath(String tagDefinitionPath) {
		this.tagDefinitionPath = tagDefinitionPath;
	}
	public ArrayList<TagRowItem> getChildren() {
		return children;
	}
	public void setChildren(ArrayList<TagRowItem> children) {
		this.children = children;
	}
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public Integer getFrequency() {
		return frequency;
	}
	public void setFrequency(int frequency) {
		
		
		this.frequency = new Integer(frequency);
	}

	public String getCollectionName() {
		return collectionName;
	}
	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
	public String getTreePath() {
		return treePath;
	}
	public void setTreePath(String treePath) {
		this.treePath = treePath;
	}
	public boolean setFrequencyOneUp() {
		if(frequency==null) {
			frequency= new Integer(1);
			
		}else {
			frequency=frequency.intValue()+1;
					
		}
	
		return true;
	}

	public String getPropertyName() {
		return propertyName;
	}
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}
	public Object getPropertyValue() {
		return propertyValue;
	}
	public void setPropertyValue(Object propertyValue) {
		this.propertyValue = propertyValue;
	}
	public QueryResultRow getQueryResultRow() {
		return queryResultRow;
	}
	public void setQueryResultRow(QueryResultRow queryResultRow) {
		this.queryResultRow = queryResultRow;
	}
	public String getPhrase() {
		return phrase;
	}
	public void setPhrase(String phrase) {
		this.phrase = 	shorten(phrase, 50);
	}
	
	
	public String getShortenTreePath(){
		return shorten(this.treePath,26);
	}
	
   private String shorten(String toShortenValue, int maxLength) {
		if (toShortenValue.length() <= maxLength) {
			return toShortenValue;
		}
		
		return toShortenValue.substring(0, maxLength/2) 
				+"["+HORIZONTAL_ELLIPSIS+"]"
				+ toShortenValue.substring(toShortenValue.length()-((maxLength/2)-2), toShortenValue.length());
	}
	
	
	
	
	
	
	
	
	




	
	
	
	

}
