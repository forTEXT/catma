package de.catma.document.repository;

import java.util.Properties;

public enum RepositoryProperties {
	INSTANCE,
	;
	private volatile Properties properties;

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}
	
	
}
