package de.catma.repository.git.managers;

import org.gitlab4j.api.models.Project;

public class ProjectExt extends Project {
	private String importStatus;

	public String getImportStatus() {
		return importStatus;
	}

	public void setImportStatus(String importStatus) {
		this.importStatus = importStatus;
	}

}
