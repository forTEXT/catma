package de.catma.project;

import java.io.IOException;
import java.util.List;

import de.catma.rbac.RBACPermission;
import de.catma.tag.TagManager;
import de.catma.user.User;

public interface ProjectManager {
	String create(String name, String description) throws Exception;

	void delete(String projectId) throws Exception;

	public User getUser();

	public List<ProjectReference> getProjectReferences() throws Exception;

	ProjectReference findProjectReferenceById(String projectId) throws IOException;

	public ProjectReference createProject(String name, String description) throws Exception;

	public void openProject(
			TagManager tagManager,
			ProjectReference projectReference,
			OpenProjectListener openProjectListener);

	void leaveProject(String projectId) throws IOException;

	boolean isAuthorizedOnProject(RBACPermission permission, String projectId);

	void updateProject(ProjectReference projectReference) throws IOException;
}
