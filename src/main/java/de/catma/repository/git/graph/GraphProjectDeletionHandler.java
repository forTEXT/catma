package de.catma.repository.git.graph;

import de.catma.project.ProjectReference;

public interface GraphProjectDeletionHandler {

	void deleteProject(ProjectReference projectReference) throws Exception;

}