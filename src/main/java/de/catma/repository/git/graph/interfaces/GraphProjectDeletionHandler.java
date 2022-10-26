package de.catma.repository.git.graph.interfaces;

import de.catma.project.ProjectReference;

public interface GraphProjectDeletionHandler {

	void deleteProject(ProjectReference projectReference) throws Exception;

}