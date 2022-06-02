package de.catma.project;

import java.io.IOException;
import java.util.List;

import de.catma.rbac.RBACPermission;
import de.catma.tag.TagManager;
import de.catma.user.User;

public interface ProjectsManager {
	void delete(String projectId) throws Exception;

	public User getUser();

	public List<ProjectReference> getProjectReferences() throws Exception;
	public List<ProjectReference> getProjectReferences(RBACPermission withPermission) throws Exception;


	public ProjectReference createProject(String name, String description) throws Exception;

	public void openProject(
			TagManager tagManager,
			ProjectReference projectReference,
			OpenProjectListener openProjectListener);

	void leaveProject(ProjectReference projectReference) throws IOException;
	boolean isAuthorizedOnProject(RBACPermission permission, ProjectReference projectReference);

	void updateProject(ProjectReference projectReference) throws IOException;

}
