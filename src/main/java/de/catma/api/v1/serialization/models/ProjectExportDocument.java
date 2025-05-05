package de.catma.api.v1.serialization.models;

import java.util.ArrayList;
import java.util.List;

import de.catma.api.v1.serialization.model_wrappers.ProjectExportAnnotation;

public class ProjectExportDocument {
	private final String id;
	private final String title;
    private final List<ProjectExportAnnotation> annotations;
    

    public ProjectExportDocument(String id, String title) {
		super();
		this.id = id;
		this.title = title;
		this.annotations = new ArrayList<>();
	}

	public String getId() {
		return id;
	}
    
    public String getTitle() {
		return title;
	}
    
    public List<ProjectExportAnnotation> getAnnotations() {
        return annotations;
    }

    public void addAnnotations(List<ProjectExportAnnotation> annotations) {
        this.annotations.addAll(annotations);
    }
}
