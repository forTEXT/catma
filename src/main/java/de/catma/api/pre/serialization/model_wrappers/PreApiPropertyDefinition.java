package de.catma.api.pre.serialization.model_wrappers;

import de.catma.tag.PropertyDefinition;

import java.util.ArrayList;
import java.util.List;

public class PreApiPropertyDefinition {
    private String id;
    private String name;
    private List<String> possibleValues;

    public PreApiPropertyDefinition() {
    }

    public PreApiPropertyDefinition(PropertyDefinition propertyDefinition) {
        id = propertyDefinition.getUuid();
        name = propertyDefinition.getName();
        possibleValues = new ArrayList<>(propertyDefinition.getPossibleValueList());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPossibleValues() {
        return possibleValues;
    }

    public void setPossibleValues(List<String> possibleValues) {
        this.possibleValues = possibleValues;
    }
}
