package de.catma.ui.analyzenew;

import java.util.ArrayList;
import java.util.UUID;

import com.vaadin.ui.CheckBox;

public class TagRowItem {
	
	private String tagID;
	private String sourceDocumentID;
	private String sourceDocName;
	private String collectionID;
	private String tagInstanceID;
	private String tagDefinitionID;
	private String tagDefinitionPath;
	private ArrayList <TagRowItem>children;
	private UUID uuid;
	private CheckBox checkBox;
	private int frequency;

	
	public TagRowItem() {
		uuid = UUID.randomUUID();
		checkBox = new CheckBox();
		
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

	public CheckBox getCheckBox() {
		return checkBox;
	}

	public void setCheckBox(CheckBox checkBox) {
		this.checkBox = checkBox;
	}
	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}
	
	




	
	
	
	

}
