package de.catma.api.pre.serialization.model_wrappers;

import java.util.List;

public class PreApiAnnotationProperty {
    private final String id;
    private final String name;
    private final List<String> values;
    
    

    public PreApiAnnotationProperty(String id, String name, List<String> values) {
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
