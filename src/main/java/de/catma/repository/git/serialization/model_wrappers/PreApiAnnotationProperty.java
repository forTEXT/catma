package de.catma.repository.git.serialization.model_wrappers;

import de.catma.tag.Property;

import java.util.ArrayList;
import java.util.List;

public class PreApiAnnotationProperty {
    private String id;
    private String name;
    private List<String> values;

    public PreApiAnnotationProperty() {
    }

    public PreApiAnnotationProperty(String name, Property property) {
        this.name = name;
        id = property.getPropertyDefinitionId();
        values = new ArrayList<>(property.getPropertyValueList());
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

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}
