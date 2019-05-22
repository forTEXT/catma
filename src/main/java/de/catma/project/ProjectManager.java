package de.catma.project;

import de.catma.Pager;
import de.catma.backgroundservice.BackgroundServiceProvider;
import de.catma.user.User;

import java.io.IOException;
import java.util.List;

public interface ProjectManager {
	String create(String name, String description) throws Exception;

	void delete(String projectId) throws Exception;

	public User getUser();

	public Pager<ProjectReference> getProjectReferences() throws Exception;

	ProjectReference findProjectReferenceById(String projectId) throws IOException;

	public ProjectReference createProject(String name, String description) throws Exception;

	public void openProject(
			ProjectReference projectReference,
			OpenProjectListener openProjectListener);

}
