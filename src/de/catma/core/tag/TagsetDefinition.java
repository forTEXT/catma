package de.catma.core.tag;

import java.util.Set;

public class TagsetDefinition implements Versionable {
	private String ID;
	private String name;
	private Version version;
	private Set<TagDefinition> tagDefinitions;
	private Set<TagDefinition> deletedTagDefinitions;
	
	public Version getVersion() {
		return version;
	}
}
