package de.catma.tag;

public class TagDefinitionPathInfo {

	private String tagDefinitionPath;
	private String tagsetDefinitionName;
	private String color;
	
	public TagDefinitionPathInfo(String tagDefinitionPath,
			String tagsetDefinitionName, String color) {
		this.tagDefinitionPath = tagDefinitionPath;
		this.tagsetDefinitionName = tagsetDefinitionName;
		this.color = color;
	}

	public String getTagDefinitionPath() {
		return tagDefinitionPath;
	}

	public String getTagsetDefinitionName() {
		return tagsetDefinitionName;
	}

	public String getColor() {
		return color;
	}
}
