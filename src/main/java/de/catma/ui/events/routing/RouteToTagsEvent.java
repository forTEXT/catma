package de.catma.ui.events.routing;

import de.catma.project.Project;
import de.catma.tag.TagsetDefinition;


public class RouteToTagsEvent {

	private final Project project;
	private final TagsetDefinition tagset;
	
	public RouteToTagsEvent(Project project) {
		this(project, null);
	}

	public RouteToTagsEvent(Project project, TagsetDefinition tagset) {
		super();
		this.project = project;
		this.tagset = tagset;
	}

	public Project getProject() {
		return project;
	}

	public TagsetDefinition getTagset() {
		return tagset;
	}
}
