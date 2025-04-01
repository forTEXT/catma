package de.catma.api.pre.serialization.model_wrappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PreApiTagDefinition {
    private final String id;
    private final String parentId;
    private final String name;
    private final transient Map<String, String> systemProperties;
    private final transient List<PreApiUserPropertyDefinition> userProperties;
    private final Map<String, Object> properties;
    private final String tagsetId;

    public PreApiTagDefinition(String id, String parentId, String name,
                               Map<String, String> systemProperties, List<PreApiUserPropertyDefinition> userProperties,
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
                PreApiUserPropertyDefinition::getId, pd -> pd
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

    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    public List<PreApiUserPropertyDefinition> getUserProperties() {
        return userProperties;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getTagsetId() {
        return tagsetId;
    }
}
