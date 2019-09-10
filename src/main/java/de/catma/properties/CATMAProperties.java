package de.catma.properties;

import java.util.Properties;

public enum CATMAProperties {
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
