package de.catma.core.document.repository;

import java.util.Properties;

public enum RepositoryPropertyKey {
	Repository,
	SerializationHandlerFactory,
	RepositoryFolderPath,
	RepositoryFactory,
	;

	public boolean isTrue(Properties properties, int index, boolean defaultValue) {
		if (properties.containsKey(this.name()+index)) {
			return isTrue(properties, index);
		}
		else {
			return defaultValue;
		}
	}
	
	public boolean isTrue(Properties properties, int index) {
		return Boolean.parseBoolean(properties.getProperty(this.name()+index));
	}
	
	public String getProperty(Properties properties, int index) {
		return properties.getProperty(this.name()+index);
	}
	
	public boolean exists(Properties properties, int index) {
		return properties.containsKey(this.name()+index);
	}
}
