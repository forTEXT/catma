package de.catma.api.pre.serialization.model_wrappers;

import java.util.List;

public class PreApiTagsetDefinition {

    private final String id;
    private final String name;
    private final String description;
	
    public PreApiTagsetDefinition(String id, String name, String description) {
		super();
		this.id = id;
		this.name = name;
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}
}
