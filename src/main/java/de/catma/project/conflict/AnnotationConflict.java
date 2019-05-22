package de.catma.project.conflict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import de.catma.document.standoffmarkup.usermarkup.TagReference;
import de.catma.tag.Property;
import de.catma.tag.TagInstance;

public class AnnotationConflict {
	
	private Resolution resolution = null;

	private TagInstance devTagInstance;
	private List<TagReference> devTagReferences;

	private TagInstance masterTagInstance;
	private List<TagReference> masterTagReferences;
	
	
	public AnnotationConflict(TagInstance devTagInstance, List<TagReference> devTagReferences,
			TagInstance masterTagInstance, List<TagReference> masterTagReferences) {
		super();
		this.devTagInstance = devTagInstance;
		this.devTagReferences = devTagReferences;
		this.masterTagInstance = masterTagInstance;
		this.masterTagReferences = masterTagReferences;
	}
	
	
	public TagInstance getDevTagInstance() {
		return devTagInstance;
	}
	
	public List<TagReference> getDevTagReferences() {
		return devTagReferences;
	}
	
	public TagInstance getMasterTagInstance() {
		return masterTagInstance;
	}
	
	public List<TagReference> getMasterTagReferences() {
		return masterTagReferences;
	}
	
	public void setResolution(Resolution resolution) {
		this.resolution = resolution;
	}
	
	public boolean isResolved() {
		return this.resolution != null;
	}
	
	public List<TagReference> getResolvedTagReferences() throws Exception {
		if (!isResolved()) {
			throw new IllegalStateException("this Annotation Conflict is not resolved yet!");
		}
		
		switch (this.resolution) {
		case MINE: {
			return getDevTagReferences();
		}
		case THEIRS: {
			return getMasterTagReferences();
		}
		default: {
			return createCombinedTagInstance();
		}
		
		}
	}


	private List<TagReference> createCombinedTagInstance() throws Exception {
		
		TagInstance combinedInstance = 
			new TagInstance(
				masterTagInstance.getUuid(), 
				masterTagInstance.getTagDefinitionId(), 
				masterTagInstance.getAuthor(), 
				masterTagInstance.getTimestamp(), 
				Collections.emptySet(), 
				masterTagInstance.getTagsetId());
		for (Property property : masterTagInstance.getUserDefinedProperties()) {
			HashSet<String> values = new HashSet<>();
			values.addAll(property.getPropertyValueList());
			Property devUserDefinedProperty = 
					devTagInstance.getUserDefinedPropetyByUuid(property.getPropertyDefinitionId());
			if (devUserDefinedProperty != null) {
				values.addAll(devUserDefinedProperty.getPropertyValueList());
			}
			combinedInstance.addUserDefinedProperty(
				new Property(property.getPropertyDefinitionId(), values));
		}
		
		List<TagReference> references = new ArrayList<>();
		for (TagReference tagReference : masterTagReferences) {
			references.add(
				new TagReference(
					combinedInstance, 
					tagReference.getTarget().toString(), 
					tagReference.getRange(), 
					tagReference.getUserMarkupCollectionUuid()));
		}
		
		return references;
	}
}
