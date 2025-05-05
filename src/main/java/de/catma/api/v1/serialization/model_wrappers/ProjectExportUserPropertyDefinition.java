package de.catma.api.v1.serialization.model_wrappers;

import java.util.List;

public class ProjectExportUserPropertyDefinition {
    private final String id;
    private final String name;
    private final List<String> possibleValues;

    public ProjectExportUserPropertyDefinition(String id, String name, List<String> possibleValues) {
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
