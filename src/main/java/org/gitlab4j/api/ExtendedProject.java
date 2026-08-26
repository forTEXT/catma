package org.gitlab4j.api;

import org.gitlab4j.api.models.Project;

/**
 * Extends {@link Project} with the <code>import_error</code> field, which upstream's model omits (as of 5.8.1). Used to surface the reason for a failed
 * project import.
 * <p>
 * See {@link ExtendedProjectApi#getExtendedProject}, which is what populates it.
 */
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
