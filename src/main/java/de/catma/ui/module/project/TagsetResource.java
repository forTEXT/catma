package de.catma.ui.module.project;

import com.vaadin.icons.VaadinIcons;

import de.catma.project.Project;
import de.catma.tag.TagsetDefinition;

public class TagsetResource implements Resource {

	private TagsetDefinition tagset;
	private String projectId;

	public TagsetResource(TagsetDefinition tagset, String projectId) {
		this.tagset = tagset;
		this.projectId = projectId;
	}

	@Override
	public String getResourceId() {
		return tagset.getUuid();
	}

	@Override
	public String getProjectId() {
		return projectId;
	}

	@Override
	public String getName() {
		return tagset.getName();
	}

	@Override
	public String getDetail() {
		return null;
	}

	@Override
	public boolean hasDetail() {
		return false;
	}

	@Override
	public String getIcon() {
		return VaadinIcons.TAGS.getHtml();
	}

	@Override
	public void deleteFrom(Project project) throws Exception {
		// noop
	}

	@Override
	public String getPermissionIcon() {
		return null; // not used
	}
	
	@Override
	public boolean hasWritePermission() {
		return false; // not used
	}

	@Override
	public boolean isTagset() {
		return true;
	}
	
	@Override
	public String getResponsableUser() {
		// TODO Auto-generated method stub
		return null;
	}
}
