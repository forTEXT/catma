package de.catma.project;

import de.catma.rbac.RBACPermission;
import de.catma.tag.TagManager;
import de.catma.user.User;

import java.io.IOException;
import java.util.List;

public interface ProjectsManager {
	enum ProjectMetadataSerializationField {
		name,
		description,
	}

	ProjectReference createProject(String name, String description) throws IOException;
	void openProject(ProjectReference projectReference, TagManager tagManager, OpenProjectListener openProjectListener);
	void updateProjectMetadata(ProjectReference projectReference) throws IOException;
	void leaveProject(ProjectReference projectReference) throws IOException;
	void deleteProject(ProjectReference projectReference) throws IOException;

	User getUser();
	List<ProjectReference> getProjectReferences() throws IOException;
	List<ProjectReference> getProjectReferences(RBACPermission withPermission) throws IOException;
	boolean isAuthorizedOnProject(ProjectReference projectReference, RBACPermission permission);
}
