package de.catma.api.pre.serialization.models;

import de.catma.api.pre.serialization.model_wrappers.PreApiAnnotation;
import de.catma.api.pre.serialization.model_wrappers.PreApiSourceDocument;
import de.catma.api.pre.serialization.model_wrappers.PreApiTagDefinition;

import java.util.List;

public class ExportDocument {
    private PreApiSourceDocument sourceDocument;
    private List<PreApiTagDefinition> tags;
    private List<PreApiAnnotation> annotations;

    public ExportDocument() {
    }

    public ExportDocument(PreApiSourceDocument preApiSourceDocument, List<PreApiTagDefinition> preApiTagDefinitions, List<PreApiAnnotation> preApiAnnotations) {
        this.sourceDocument = preApiSourceDocument;
        this.tags = preApiTagDefinitions;
        this.annotations = preApiAnnotations;
    }

    public PreApiSourceDocument getSourceDocument() {
        return sourceDocument;
    }

    public void setSourceDocument(PreApiSourceDocument sourceDocument) {
        this.sourceDocument = sourceDocument;
    }

    public List<PreApiTagDefinition> getTags() {
        return tags;
    }

    public void setTags(List<PreApiTagDefinition> tags) {
        this.tags = tags;
    }

    public List<PreApiAnnotation> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<PreApiAnnotation> annotations) {
        this.annotations = annotations;
    }
}
