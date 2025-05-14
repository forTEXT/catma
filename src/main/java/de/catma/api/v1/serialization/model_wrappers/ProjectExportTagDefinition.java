package de.catma.api.v1.serialization.model_wrappers;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProjectExportTagDefinition {
    private final String id;
    private final String parentId;
    private final String name;
    private final transient Map<String, String> systemProperties;
    private final transient List<ProjectExportUserPropertyDefinition> userProperties;
    private final Map<String, Object> properties;
    private final String tagsetId;

    public ProjectExportTagDefinition(String id, String parentId, String name,
                                      Map<String, String> systemProperties, List<ProjectExportUserPropertyDefinition> userProperties,
                                      String tagsetId) {
        super();
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.systemProperties = systemProperties;
        this.userProperties = userProperties;
        this.tagsetId = tagsetId;

        this.properties = new HashMap<>();
        this.properties.put("system", this.systemProperties);
        this.properties.put("user", this.userProperties.stream().collect(Collectors.toMap(
                ProjectExportUserPropertyDefinition::getId, pd -> pd
        )));
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

    @JsonIgnore
    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    @JsonIgnore
    public List<ProjectExportUserPropertyDefinition> getUserProperties() {
        return userProperties;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getTagsetId() {
        return tagsetId;
    }
}
