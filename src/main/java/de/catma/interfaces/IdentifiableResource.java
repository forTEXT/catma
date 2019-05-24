package de.catma.interfaces;

public interface IdentifiableResource {
	
	/**
	 * 
	 * @return the resource identifier e.g. catma uuid
	 */
	String getResourceId();

	
	/**
	 * 
	 * @return the project identifier
	 */
	String getProjectId();
	
}
