package de.catma.repository.git.interfaces;

import de.catma.repository.IProjectHandler;
import de.catma.repository.git.exceptions.ProjectHandlerException;

public interface IGitBasedProjectHandler extends IProjectHandler {
	String getRootRepositoryHttpUrl(int projectId) throws ProjectHandlerException;
}
