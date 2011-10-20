package de.catma.core.tag;

import java.util.List;

public class TagDefinition implements Versionable {
	
	private String ID;
	private String type;
	private Version version;
	private String description;
	private List<PropertyDefinition> systemPropertyDefinitions;
	private List<PropertyDefinition> userDefinedPropertyDefinitions;
	
	
	
	
	public Version getVersion() {
		return version;
	}
	
	
	
}
