package de.catma.api.pre.serialization.models;

import java.util.List;

import de.catma.api.pre.serialization.model_wrappers.PreApiAnnotation;

public class ExportDocument {
	private final String id;
	private final String title;
    private final List<PreApiAnnotation> annotations;
    

    public ExportDocument(String id, String title, List<PreApiAnnotation> annotations) {
		super();
		this.id = id;
		this.title = title;
		this.annotations = annotations;
	}

	public String getId() {
		return id;
	}
    
    public String getTitle() {
		return title;
	}
    
    public List<PreApiAnnotation> getAnnotations() {
        return annotations;
    }
}
