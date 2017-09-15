package de.catma.repository;

import de.catma.repository.git.exceptions.ProjectHandlerException;

public interface IProjectHandler {
	int create(String name, String description) throws ProjectHandlerException;
	void delete(int projectId) throws ProjectHandlerException;
}
