package de.catma.api.pre.serialization.models;

import de.catma.api.pre.serialization.model_wrappers.LegacyPreApiAnnotation;
import de.catma.api.pre.serialization.model_wrappers.LegacyPreApiTagDefinition;
import de.catma.api.pre.serialization.model_wrappers.PreApiSourceDocument;

import java.util.List;

@Deprecated
public class LegacyExportDocument {
    private final PreApiSourceDocument sourceDocument;
    private final List<LegacyPreApiTagDefinition> tags;
    private final List<LegacyPreApiAnnotation> annotations;

    public LegacyExportDocument(PreApiSourceDocument preApiSourceDocument, List<LegacyPreApiTagDefinition> legacyPreApiTagDefinitions, List<LegacyPreApiAnnotation> legacyPreApiAnnotations) {
        this.sourceDocument = preApiSourceDocument;
        this.tags = legacyPreApiTagDefinitions;
        this.annotations = legacyPreApiAnnotations;
    }

    public PreApiSourceDocument getSourceDocument() {
        return sourceDocument;
    }

    public List<LegacyPreApiTagDefinition> getTags() {
        return tags;
    }

    public List<LegacyPreApiAnnotation> getAnnotations() {
        return annotations;
    }
}
