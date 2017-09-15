package de.catma.repository;

import de.catma.repository.git.exceptions.ProjectHandlerException;

public interface IGitBasedProjectHandler extends IProjectHandler {
	String getRootRepositoryHttpUrl(int projectId) throws ProjectHandlerException;
}
