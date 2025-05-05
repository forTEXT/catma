package de.catma.api.v1.serialization.models;

import de.catma.api.v1.serialization.model_wrappers.LegacyPreApiAnnotation;
import de.catma.api.v1.serialization.model_wrappers.LegacyPreApiTagDefinition;
import de.catma.api.v1.serialization.model_wrappers.ProjectExportSourceDocument;

import java.util.List;

@Deprecated
public class LegacyExportDocument {
    private final ProjectExportSourceDocument sourceDocument;
    private final List<LegacyPreApiTagDefinition> tags;
    private final List<LegacyPreApiAnnotation> annotations;

    public LegacyExportDocument(ProjectExportSourceDocument projectExportSourceDocument, List<LegacyPreApiTagDefinition> legacyPreApiTagDefinitions, List<LegacyPreApiAnnotation> legacyPreApiAnnotations) {
        this.sourceDocument = projectExportSourceDocument;
        this.tags = legacyPreApiTagDefinitions;
        this.annotations = legacyPreApiAnnotations;
    }

    public ProjectExportSourceDocument getSourceDocument() {
        return sourceDocument;
    }

    public List<LegacyPreApiTagDefinition> getTags() {
        return tags;
    }

    public List<LegacyPreApiAnnotation> getAnnotations() {
        return annotations;
    }
}
