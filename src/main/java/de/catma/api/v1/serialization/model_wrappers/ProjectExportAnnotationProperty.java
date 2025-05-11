package de.catma.api.v1.serialization.model_wrappers;

import java.util.List;

public class ProjectExportAnnotationProperty {
    private final String id;
    private final String name;
    private final List<String> values;
    
    

    public ProjectExportAnnotationProperty(String id, String name, List<String> values) {
		super();
		this.id = id;
		this.name = name;
		this.values = values;
	}

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getValues() {
        return values;
    }
}
