package de.catma.api.pre.serialization.model_wrappers;

import java.util.List;

public class PreApiTagDefinition {
    private final String id;
    private final String parentId;
    private final String name;
    private final String colour;
    private final List<PreApiPropertyDefinition> properties;

    public PreApiTagDefinition(String id, String parentId, String name, String colour,
			List<PreApiPropertyDefinition> properties) {
		super();
		this.id = id;
		this.parentId = parentId;
		this.name = name;
		this.colour = colour;
		this.properties = properties;
	}

    public String getId() {
        return id;
    }

    public String getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public String getColour() {
        return colour;
    }

    public List<PreApiPropertyDefinition> getProperties() {
        return properties;
    }
}
