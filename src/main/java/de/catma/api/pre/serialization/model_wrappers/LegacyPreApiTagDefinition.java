package de.catma.api.pre.serialization.model_wrappers;

import java.util.List;

@Deprecated
public class LegacyPreApiTagDefinition {
    private final String id;
    private final String parentId;
    private final String name;
    private final String colour;
    private final List<PreApiUserPropertyDefinition> properties;

    public LegacyPreApiTagDefinition(String id, String parentId, String name, String colour, List<PreApiUserPropertyDefinition> properties) {
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

    public List<PreApiUserPropertyDefinition> getProperties() {
        return properties;
    }
}
