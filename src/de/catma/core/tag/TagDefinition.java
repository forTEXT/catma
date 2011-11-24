package de.catma.core.tag;

import java.util.List;

public class TagDefinition implements Versionable {
	
	// TODO: hier besser interface einziehen um Links als tagdefs abzubilden.
	
	private String ID;
	private String type;
	private Version version;
	private String description;
	private List<PropertyDefinition> systemPropertyDefinitions;
	private List<PropertyDefinition> userDefinedPropertyDefinitions;
	private TagDefinition parent;


	
	
	
	public Version getVersion() {
		return version;
	}
	
	
	
}
