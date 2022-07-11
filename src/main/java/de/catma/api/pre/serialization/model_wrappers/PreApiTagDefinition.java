package de.catma.api.pre.serialization.model_wrappers;

import de.catma.tag.TagDefinition;

import java.util.List;
import java.util.stream.Collectors;

public class PreApiTagDefinition {
    private String id;
    private String parentId;
    private String name;
    private String colour;
    private List<PreApiPropertyDefinition> properties;

    public PreApiTagDefinition() {
    }

    public PreApiTagDefinition(TagDefinition tagDefinition) {
        id = tagDefinition.getUuid();
        parentId = tagDefinition.getParentUuid();
        name = tagDefinition.getName();
        colour = tagDefinition.getHexColor();
        properties = tagDefinition.getUserDefinedPropertyDefinitions().stream().map(PreApiPropertyDefinition::new).collect(Collectors.toList());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public List<PreApiPropertyDefinition> getProperties() {
        return properties;
    }

    public void setProperties(List<PreApiPropertyDefinition> properties) {
        this.properties = properties;
    }
}
