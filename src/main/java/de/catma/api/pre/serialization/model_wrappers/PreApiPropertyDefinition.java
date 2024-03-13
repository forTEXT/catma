package de.catma.api.pre.serialization.model_wrappers;

import java.util.List;

public class PreApiPropertyDefinition {
    private final String id;
    private final String name;
    private final List<String> possibleValues;

    public PreApiPropertyDefinition(String id, String name, List<String> possibleValues) {
		super();
		this.id = id;
		this.name = name;
		this.possibleValues = possibleValues;
	}

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getPossibleValues() {
        return possibleValues;
    }
}
