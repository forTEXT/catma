package de.catma.core.tag;

import java.util.List;

public class TagInstance {

	private String ID;
	private TagDefinition tagDefinition;
	private List<Property> systemProperties; // TODO: store user as system properties
	private List<Property> userDefinedProperties;
}
