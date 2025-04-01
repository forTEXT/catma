package de.catma.api.pre.serialization.model_wrappers;

public class PreApiTagsetDefinition {

    private final String id;
    private final String name;
    private final String description;
    private final String responsibleUser;
	
    public PreApiTagsetDefinition(String id, String name, String description, String responsibleUser) {
		super();
		this.id = id;
		this.name = name;
		this.description = description;
		this.responsibleUser = responsibleUser;
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

	public String getResponsibleUser() {
		return responsibleUser;
	}
}
