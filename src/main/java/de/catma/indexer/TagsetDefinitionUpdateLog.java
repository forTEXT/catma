package de.catma.indexer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TagsetDefinitionUpdateLog {
	
	private Set<String> updatedTagDefinitionUuids = new HashSet<String>();
	private Set<String> deletedTagDefinitionUuids = new HashSet<String>();
	private Map<String, Set<String>> updatedPropertyDefinitionUuids = new HashMap<String, Set<String>>();
	private Set<String> deletedPropertyDefinitionUuids = new HashSet<String>();
	
	
	public Set<String> getUpdatedTagDefinitionUuids() {
		return updatedTagDefinitionUuids;
	}
	
	public Set<String> getDeletedTagDefinitionUuids() {
		return deletedTagDefinitionUuids;
	}
	
	public Map<String, Set<String>> getUpdatedPropertyDefinitionUuids() {
		return updatedPropertyDefinitionUuids;
	}
	
	public Set<String> getDeletedPropertyDefinitionUuids() {
		return deletedPropertyDefinitionUuids;
	}
	
	
	
	public void setDeletedTagDefinitionUuids(Set<String> deletedTagDefinitionUuids) {
		this.deletedTagDefinitionUuids = deletedTagDefinitionUuids;
	}
	
	public void addUpdatedTagDefinition(String tagDefUuid) {
		updatedTagDefinitionUuids.add(tagDefUuid);
	}
	
	public void addUpdatedPropertyDefinition(String propDefUuid, String tagDefUuid) {
		Set<String> updatedPropDefSet = null;
		if (!updatedPropertyDefinitionUuids.containsKey(tagDefUuid)) {
			updatedPropDefSet = new HashSet<String>();
			updatedPropertyDefinitionUuids.put(tagDefUuid, updatedPropDefSet);
		}
		else {
			updatedPropDefSet = updatedPropertyDefinitionUuids.get(tagDefUuid);
		}
		
		updatedPropDefSet.add(propDefUuid);
	}
	
	public void addDeletedPropertyDefinitions(Collection<String> propDefUuids) {
		deletedPropertyDefinitionUuids.addAll(propDefUuids);
	}
	
	
	public boolean isEmpty() {
		return deletedTagDefinitionUuids.isEmpty() && updatedTagDefinitionUuids.isEmpty()
				&& deletedPropertyDefinitionUuids.isEmpty()
				&& updatedPropertyDefinitionUuids.isEmpty(); 
	}
}
