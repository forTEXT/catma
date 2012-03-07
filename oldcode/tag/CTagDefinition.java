package de.catma.ui.client.ui.tag;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CTagDefinition implements CVersionable, DisplayableTagChild {
	
	// TODO: hier besser interface einziehen um Links als tagdefs abzubilden.
	
	private static enum SystemPropertyName {
		catma_displaycolor,
		;
	}
	
	public final static CTagDefinition CATMA_BASE_TAG = 
			new CTagDefinition(
				"CATMA_BASE_TAG", "CATMA_BASE_TAG", new CVersion("1"), null);
	
	private String id;
	private String type;
	private CVersion version;
	private Map<String,CPropertyDefinition> systemPropertyDefinitions;
	private Map<String,CPropertyDefinition> userDefinedPropertyDefinitions;
	private String baseID;


	public CTagDefinition(String id, String type, CVersion version, String baseID) {
		super();
		this.id = id;
		this.type = type;
		this.version = version;
		this.baseID = baseID;
		if ((this.baseID != null) && this.baseID.isEmpty()) {
			this.baseID = null;
		}
		systemPropertyDefinitions = new HashMap<String, CPropertyDefinition>();
		userDefinedPropertyDefinitions = new HashMap<String, CPropertyDefinition>();
	}

	public CVersion getVersion() {
		return version;
	}
	
	
	@Override
	public String toString() {
		return "TAG_DEF[" + type 
				+ ",#" + id +","
				+version
				+((baseID==null) ? "]" : (",#"+baseID+"]"));
	}

	public void addSystemPropertyDefinition(CPropertyDefinition propertyDefinition) {
		systemPropertyDefinitions.put(propertyDefinition.getName(), propertyDefinition);
	}
	
	public void addUserDefinedPropertyDefinition(CPropertyDefinition propertyDefinition) {
		userDefinedPropertyDefinitions.put(propertyDefinition.getName(), propertyDefinition);
	}	
	
	public String getID() {
		return id;
	}

	public CPropertyDefinition getPropertyDefinition(String propertyName) {
		if (systemPropertyDefinitions.containsKey(propertyName)) {
			return systemPropertyDefinitions.get(propertyName);
		}
		else {
			return userDefinedPropertyDefinitions.get(propertyName);
		}
	}
	
	public Collection<CPropertyDefinition> getUserDefinedPropertyDefinitions() {
		return Collections.unmodifiableCollection(
				userDefinedPropertyDefinitions.values());
	}
	
	public String getBaseID() {
		return baseID;
	}
	
	public String getType() {
		return type;
	}
	
	public String getColor() {
		return systemPropertyDefinitions.get(
				SystemPropertyName.catma_displaycolor.name()).getFirstValue();
	}

	public Collection<CPropertyDefinition> getSystemPropertyDefinitions() {
		return Collections.unmodifiableCollection(
				systemPropertyDefinitions.values());
	}
	
	public String getDisplayString() {
		return getType();
	}
}
