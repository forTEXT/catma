package org.gitlab4j.api;

import org.gitlab4j.api.models.Project;

public class ExtendedProject extends Project {

	private String importError;
	
	public ExtendedProject() {
		super();
	}
	
	public void setImportError(String importError) {
		this.importError = importError;
	}
	
	public String getImportError() {
		return importError;
	}
	
}
