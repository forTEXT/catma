package de.catma.api.pre.serialization.models;

import de.catma.api.pre.serialization.model_wrappers.PreApiAnnotation;
import de.catma.api.pre.serialization.model_wrappers.PreApiSourceDocument;
import de.catma.api.pre.serialization.model_wrappers.PreApiTagDefinition;

import java.util.List;

@Deprecated
public class LegacyExportDocument {
    private final PreApiSourceDocument sourceDocument;
    private final List<PreApiTagDefinition> tags;
    private final List<PreApiAnnotation> annotations;

    public LegacyExportDocument(PreApiSourceDocument preApiSourceDocument, List<PreApiTagDefinition> preApiTagDefinitions, List<PreApiAnnotation> preApiAnnotations) {
        this.sourceDocument = preApiSourceDocument;
        this.tags = preApiTagDefinitions;
        this.annotations = preApiAnnotations;
    }

    public PreApiSourceDocument getSourceDocument() {
        return sourceDocument;
    }

    public List<PreApiTagDefinition> getTags() {
        return tags;
    }

    public List<PreApiAnnotation> getAnnotations() {
        return annotations;
    }
}
