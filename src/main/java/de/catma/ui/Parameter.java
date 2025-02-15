package de.catma.ui;

public enum Parameter {
	EXPERT("catma.expert"),
	TOKEN("token"), // the token argument evaluated by the given action
	ACTION("action"), // the action to execute after authentication (joingroup, joinproject, verify) 
	;
	
	private String key;

	private Parameter(String key) {
		this.key = key;
	}
	
	public String getKey() {
		return key;
	}
}
